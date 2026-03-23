package com.blog.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.server.entity.*;
import com.blog.server.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Welcome email sequence automation.
 *
 * When a subscriber confirms, all active sequence steps are enqueued
 * with calculated send times (confirmedAt + delayDays).
 * A scheduled task runs every 5 minutes to send due emails.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WelcomeSequenceService {

    private final WelcomeSequenceTemplateMapper templateMapper;
    private final WelcomeSequenceLogMapper logMapper;
    private final SubscriberMapper subscriberMapper;
    private final ArticleMapper articleMapper;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@blog.com}")
    private String fromEmail;

    @Value("${app.base-url:http://localhost:5173}")
    private String baseUrl;

    /**
     * Enqueue all active welcome sequence steps for a new subscriber.
     * Called when subscriber confirms their email.
     */
    public void enqueueForSubscriber(Subscriber subscriber) {
        List<WelcomeSequenceTemplate> templates = templateMapper.selectList(
                new LambdaQueryWrapper<WelcomeSequenceTemplate>()
                        .eq(WelcomeSequenceTemplate::getIsActive, true)
                        .orderByAsc(WelcomeSequenceTemplate::getStepOrder));

        if (templates.isEmpty()) return;

        LocalDateTime confirmedAt = subscriber.getConfirmedAt() != null
                ? subscriber.getConfirmedAt() : LocalDateTime.now();

        for (WelcomeSequenceTemplate template : templates) {
            // Check if already enqueued (idempotent)
            Long existing = logMapper.selectCount(
                    new LambdaQueryWrapper<WelcomeSequenceLog>()
                            .eq(WelcomeSequenceLog::getSubscriberId, subscriber.getId())
                            .eq(WelcomeSequenceLog::getTemplateId, template.getId()));
            if (existing > 0) continue;

            WelcomeSequenceLog seqLog = new WelcomeSequenceLog();
            seqLog.setSubscriberId(subscriber.getId());
            seqLog.setTemplateId(template.getId());
            seqLog.setStepOrder(template.getStepOrder());
            seqLog.setStatus("PENDING");
            seqLog.setScheduledAt(confirmedAt.plusDays(template.getDelayDays()));
            logMapper.insert(seqLog);
        }

        log.info("Enqueued {} welcome sequence steps for subscriber #{}", templates.size(), subscriber.getId());
    }

    /**
     * Scheduled task — runs every 5 minutes.
     * Finds due PENDING sequence emails and sends them.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void processScheduledEmails() {
        List<WelcomeSequenceLog> dueLogs = logMapper.selectList(
                new LambdaQueryWrapper<WelcomeSequenceLog>()
                        .eq(WelcomeSequenceLog::getStatus, "PENDING")
                        .le(WelcomeSequenceLog::getScheduledAt, LocalDateTime.now())
                        .orderByAsc(WelcomeSequenceLog::getScheduledAt)
                        .last("LIMIT 100"));

        if (dueLogs.isEmpty()) return;

        log.info("Processing {} due welcome sequence emails", dueLogs.size());

        for (WelcomeSequenceLog seqLog : dueLogs) {
            try {
                Subscriber subscriber = subscriberMapper.selectById(seqLog.getSubscriberId());
                // Skip if unsubscribed
                if (subscriber == null || !"CONFIRMED".equals(subscriber.getStatus())) {
                    seqLog.setStatus("SKIPPED");
                    logMapper.updateById(seqLog);
                    continue;
                }

                WelcomeSequenceTemplate template = templateMapper.selectById(seqLog.getTemplateId());
                if (template == null || !Boolean.TRUE.equals(template.getIsActive())) {
                    seqLog.setStatus("SKIPPED");
                    logMapper.updateById(seqLog);
                    continue;
                }

                // Personalize and send
                String html = personalizeEmail(template, subscriber);
                sendEmail(subscriber.getEmail(), template.getSubject(), html);

                seqLog.setStatus("SENT");
                seqLog.setSentAt(LocalDateTime.now());
                logMapper.updateById(seqLog);

                log.info("Welcome step {} sent to {}", template.getStepOrder(), subscriber.getEmail());
            } catch (Exception e) {
                seqLog.setStatus("FAILED");
                seqLog.setErrorMsg(e.getMessage());
                logMapper.updateById(seqLog);
                log.warn("Welcome step {} failed for subscriber #{}: {}",
                        seqLog.getStepOrder(), seqLog.getSubscriberId(), e.getMessage());
            }
        }
    }

    /**
     * Get sequence status for a subscriber (admin view).
     */
    public List<Map<String, Object>> getSequenceStatus(Long subscriberId) {
        List<WelcomeSequenceLog> logs = logMapper.selectList(
                new LambdaQueryWrapper<WelcomeSequenceLog>()
                        .eq(WelcomeSequenceLog::getSubscriberId, subscriberId)
                        .orderByAsc(WelcomeSequenceLog::getStepOrder));

        return logs.stream().map(l -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("stepOrder", l.getStepOrder());
            m.put("status", l.getStatus());
            m.put("scheduledAt", l.getScheduledAt());
            m.put("sentAt", l.getSentAt());
            m.put("error", l.getErrorMsg());
            return m;
        }).collect(Collectors.toList());
    }

    /**
     * List all templates (admin).
     */
    public List<WelcomeSequenceTemplate> listTemplates() {
        return templateMapper.selectList(
                new LambdaQueryWrapper<WelcomeSequenceTemplate>()
                        .orderByAsc(WelcomeSequenceTemplate::getStepOrder));
    }

    /**
     * Update a template (admin).
     */
    public void updateTemplate(WelcomeSequenceTemplate template) {
        template.setUpdatedAt(LocalDateTime.now());
        templateMapper.updateById(template);
    }

    // ============ Helpers ============

    private String personalizeEmail(WelcomeSequenceTemplate template, Subscriber subscriber) {
        String html = template.getContentHtml();
        html = html.replace("{{BASE_URL}}", baseUrl);
        html = html.replace("{{UNSUB_TOKEN}}", subscriber.getConfirmToken() != null ? subscriber.getConfirmToken() : "");
        html = html.replace("{{SUBSCRIBER_NAME}}", subscriber.getName() != null ? subscriber.getName() : "Reader");

        // Build recommended articles HTML if template has article IDs
        if (template.getRecommendArticleIds() != null && !template.getRecommendArticleIds().isEmpty()) {
            String articlesHtml = buildRecommendedArticlesHtml(template.getRecommendArticleIds());
            html = html.replace("{{RECOMMENDED_ARTICLES}}", articlesHtml);
        } else {
            // Auto-pick top 3 popular articles
            String articlesHtml = buildPopularArticlesHtml();
            html = html.replace("{{RECOMMENDED_ARTICLES}}", articlesHtml);
        }

        return html;
    }

    private String buildRecommendedArticlesHtml(String articleIds) {
        try {
            List<Long> ids = Arrays.stream(articleIds.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(Collectors.toList());

            if (ids.isEmpty()) return buildPopularArticlesHtml();

            List<Article> articles = articleMapper.selectList(
                    new LambdaQueryWrapper<Article>()
                            .in(Article::getId, ids)
                            .eq(Article::getStatus, "PUBLISHED"));
            return formatArticlesList(articles);
        } catch (Exception e) {
            return buildPopularArticlesHtml();
        }
    }

    private String buildPopularArticlesHtml() {
        List<Article> articles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getStatus, "PUBLISHED")
                        .orderByDesc(Article::getViewCount)
                        .last("LIMIT 3"));
        return formatArticlesList(articles);
    }

    private String formatArticlesList(List<Article> articles) {
        if (articles.isEmpty()) return "<p style='color:#888;'>Check out our latest articles on the blog!</p>";

        StringBuilder sb = new StringBuilder();
        for (Article a : articles) {
            sb.append(String.format(
                    "<div style='padding:12px 0;border-bottom:1px solid #1e1e3a;'>" +
                    "<a href='%s/article/%s' style='color:#818cf8;text-decoration:none;font-size:16px;font-weight:600;'>%s</a>" +
                    "<p style='color:#888;font-size:14px;margin:4px 0 0;'>%s</p></div>",
                    baseUrl, a.getSlug(), a.getTitle(),
                    a.getExcerpt() != null ? a.getExcerpt() : ""));
        }
        return sb.toString();
    }

    @SuppressWarnings("null")
    private void sendEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.debug("Welcome sequence email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Email send failed: " + e.getMessage(), e);
        }
    }
}
