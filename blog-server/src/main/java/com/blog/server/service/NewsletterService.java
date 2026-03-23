package com.blog.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.blog.server.entity.*;
import com.blog.server.exception.BusinessException;
import com.blog.server.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.mail.internet.MimeMessage;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Newsletter Service — create, send (async batch), track, and report.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NewsletterService {

    private final NewsletterTaskMapper taskMapper;
    private final NewsletterLogMapper logMapper;
    private final SubscriberMapper subscriberMapper;
    private final ArticleMapper articleMapper;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@blog.com}")
    private String fromEmail;

    @Value("${app.base-url:http://localhost:5173}")
    private String baseUrl;

    /**
     * Create newsletter from a published article.
     */
    public NewsletterTask createFromArticle(Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) throw new BusinessException(404, "Article not found");

        NewsletterTask task = new NewsletterTask();
        task.setArticleId(articleId);
        task.setSubject(article.getTitle());
        task.setContent(article.getExcerpt());
        task.setContentHtml(buildArticleEmail(article));
        task.setAudienceType("ALL");
        task.setStatus("DRAFT");
        task.setTotalCount(0);
        task.setSentCount(0);
        task.setFailedCount(0);
        taskMapper.insert(task);
        return task;
    }

    /**
     * Create a manual newsletter (not linked to any article).
     */
    public NewsletterTask createManual(String subject, String contentHtml, String audienceType) {
        NewsletterTask task = new NewsletterTask();
        task.setSubject(subject);
        task.setContentHtml(contentHtml);
        task.setAudienceType(audienceType != null ? audienceType : "ALL");
        task.setStatus("DRAFT");
        task.setTotalCount(0);
        task.setSentCount(0);
        task.setFailedCount(0);
        taskMapper.insert(task);
        return task;
    }

    /**
     * Schedule a newsletter for future sending.
     */
    public void schedule(Long taskId, LocalDateTime scheduledAt) {
        taskMapper.update(null, new LambdaUpdateWrapper<NewsletterTask>()
                .eq(NewsletterTask::getId, taskId)
                .set(NewsletterTask::getScheduledAt, scheduledAt)
                .set(NewsletterTask::getStatus, "SCHEDULED"));
    }

    /**
     * Send newsletter asynchronously in batches.
     * This is the core send logic — runs in a background thread.
     */
    @Async
    public void sendAsync(Long taskId) {
        NewsletterTask task = taskMapper.selectById(taskId);
        if (task == null) return;

        // Update status to SENDING
        task.setStatus("SENDING");
        taskMapper.updateById(task);

        // Get target audience
        List<Subscriber> subscribers = getAudience(task.getAudienceType());
        if (subscribers.isEmpty()) {
            task.setStatus("SENT");
            task.setTotalCount(0);
            task.setSentCount(0);
            task.setSentAt(LocalDateTime.now());
            taskMapper.updateById(task);
            return;
        }

        task.setTotalCount(subscribers.size());
        taskMapper.updateById(task);

        int sentCount = 0;
        int failedCount = 0;

        // Batch send — 50 at a time
        for (int i = 0; i < subscribers.size(); i++) {
            Subscriber sub = subscribers.get(i);
            NewsletterLog sendLog = new NewsletterLog();
            sendLog.setTaskId(taskId);
            sendLog.setSubscriberId(sub.getId());
            sendLog.setOpened(false);
            sendLog.setClicked(false);

            try {
                // Simulate email send — in production, integrate with SMTP/SES/Mailgun
                sendEmail(sub.getEmail(), task.getSubject(), task.getContentHtml(), sub.getConfirmToken());
                sendLog.setStatus("SENT");
                sendLog.setSentAt(LocalDateTime.now());
                sentCount++;
            } catch (Exception e) {
                sendLog.setStatus("FAILED");
                sendLog.setErrorMsg(e.getMessage());
                failedCount++;
                log.warn("Failed to send newsletter to {}: {}", sub.getEmail(), e.getMessage());
            }

            logMapper.insert(sendLog);

            // Update progress every 50
            if ((i + 1) % 50 == 0) {
                task.setSentCount(sentCount);
                task.setFailedCount(failedCount);
                taskMapper.updateById(task);
            }
        }

        // Final update
        task.setSentCount(sentCount);
        task.setFailedCount(failedCount);
        task.setStatus(failedCount == 0 ? "SENT" : (sentCount > 0 ? "SENT" : "FAILED"));
        task.setSentAt(LocalDateTime.now());
        taskMapper.updateById(task);

        log.info("Newsletter #{} sent: {} success, {} failed out of {}",
                taskId, sentCount, failedCount, subscribers.size());
    }

    /**
     * Retry failed sends for a task.
     */
    @Async
    public void retryFailed(Long taskId) {
        List<NewsletterLog> failedLogs = logMapper.selectList(
                new LambdaQueryWrapper<NewsletterLog>()
                        .eq(NewsletterLog::getTaskId, taskId)
                        .eq(NewsletterLog::getStatus, "FAILED"));

        if (failedLogs.isEmpty()) return;

        NewsletterTask task = taskMapper.selectById(taskId);
        if (task == null) return;

        int retried = 0;
        for (NewsletterLog failedLog : failedLogs) {
            Subscriber sub = subscriberMapper.selectById(failedLog.getSubscriberId());
            if (sub == null || !"CONFIRMED".equals(sub.getStatus())) continue;

            try {
                sendEmail(sub.getEmail(), task.getSubject(), task.getContentHtml(), sub.getConfirmToken());
                failedLog.setStatus("SENT");
                failedLog.setSentAt(LocalDateTime.now());
                failedLog.setErrorMsg(null);
                retried++;
            } catch (Exception e) {
                failedLog.setErrorMsg("Retry failed: " + e.getMessage());
            }
            logMapper.updateById(failedLog);
        }

        // Update task counts
        task.setSentCount((task.getSentCount() != null ? task.getSentCount() : 0) + retried);
        task.setFailedCount(Math.max(0, (task.getFailedCount() != null ? task.getFailedCount() : 0) - retried));
        taskMapper.updateById(task);
    }

    /**
     * Track email open (called via tracking pixel).
     */
    public void trackOpen(Long logId) {
        logMapper.update(null, new LambdaUpdateWrapper<NewsletterLog>()
                .eq(NewsletterLog::getId, logId)
                .set(NewsletterLog::getOpened, true)
                .set(NewsletterLog::getOpenedAt, LocalDateTime.now()));
    }

    /**
     * Track link click (called via redirect endpoint).
     */
    public void trackClick(Long logId) {
        logMapper.update(null, new LambdaUpdateWrapper<NewsletterLog>()
                .eq(NewsletterLog::getId, logId)
                .set(NewsletterLog::getClicked, true)
                .set(NewsletterLog::getClickedAt, LocalDateTime.now()));
    }

    /**
     * Get send stats for a newsletter task.
     */
    public Map<String, Object> getStats(Long taskId) {
        NewsletterTask task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException(404, "Newsletter not found");

        Long totalLogs = logMapper.selectCount(
                new LambdaQueryWrapper<NewsletterLog>().eq(NewsletterLog::getTaskId, taskId));
        Long sentLogs = logMapper.selectCount(
                new LambdaQueryWrapper<NewsletterLog>()
                        .eq(NewsletterLog::getTaskId, taskId)
                        .eq(NewsletterLog::getStatus, "SENT"));
        Long failedLogs = logMapper.selectCount(
                new LambdaQueryWrapper<NewsletterLog>()
                        .eq(NewsletterLog::getTaskId, taskId)
                        .eq(NewsletterLog::getStatus, "FAILED"));
        Long opened = logMapper.selectCount(
                new LambdaQueryWrapper<NewsletterLog>()
                        .eq(NewsletterLog::getTaskId, taskId)
                        .eq(NewsletterLog::getOpened, true));
        Long clicked = logMapper.selectCount(
                new LambdaQueryWrapper<NewsletterLog>()
                        .eq(NewsletterLog::getTaskId, taskId)
                        .eq(NewsletterLog::getClicked, true));

        double openRate = sentLogs > 0 ? Math.round((opened * 100.0 / sentLogs) * 10) / 10.0 : 0;
        double clickRate = sentLogs > 0 ? Math.round((clicked * 100.0 / sentLogs) * 10) / 10.0 : 0;

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("taskId", taskId);
        stats.put("subject", task.getSubject());
        stats.put("status", task.getStatus());
        stats.put("total", totalLogs);
        stats.put("sent", sentLogs);
        stats.put("failed", failedLogs);
        stats.put("opened", opened);
        stats.put("clicked", clicked);
        stats.put("openRate", openRate + "%");
        stats.put("clickRate", clickRate + "%");
        stats.put("sentAt", task.getSentAt());
        return stats;
    }

    // ============ Helpers ============

    private List<Subscriber> getAudience(String audienceType) {
        LambdaQueryWrapper<Subscriber> wrapper = new LambdaQueryWrapper<Subscriber>()
                .eq(Subscriber::getStatus, "CONFIRMED");

        if (StringUtils.hasText(audienceType) && audienceType.startsWith("TAG:")) {
            String tag = audienceType.substring(4);
            wrapper.like(Subscriber::getTags, tag);
        }
        // ALL, FREE — send to all confirmed subscribers
        // PREMIUM filtering can be added when payment system is in place

        return subscriberMapper.selectList(wrapper);
    }

    /**
     * Build HTML email from article data.
     * Includes unsubscribe link with subscriber's confirm token.
     */
    private String buildArticleEmail(Article article) {
        String title = article.getTitle() != null ? article.getTitle() : "New Post";
        String excerpt = article.getExcerpt() != null ? article.getExcerpt() : "";
        String slug = article.getSlug() != null ? article.getSlug() : "";

        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="utf-8"><meta name="viewport" content="width=device-width"></head>
                <body style="margin:0;padding:0;background:#0f0f23;font-family:-apple-system,sans-serif;">
                <div style="max-width:600px;margin:0 auto;padding:32px 24px;">
                  <h1 style="color:#fff;font-size:24px;margin-bottom:16px;">%s</h1>
                  <p style="color:#a0a0b0;font-size:16px;line-height:1.6;">%s</p>
                  <a href="{{BASE_URL}}/article/%s"
                     style="display:inline-block;margin-top:24px;padding:12px 28px;background:#6366f1;color:#fff;text-decoration:none;border-radius:8px;font-weight:600;">
                     Read Full Article →
                  </a>
                  <hr style="border:1px solid #1e1e3a;margin:32px 0;">
                  <p style="color:#555;font-size:12px;">
                    You received this because you subscribed to our newsletter.<br>
                    <a href="{{BASE_URL}}/unsubscribe?token={{UNSUB_TOKEN}}" style="color:#6366f1;">Unsubscribe</a>
                  </p>
                </div>
                </body>
                </html>
                """.formatted(title, excerpt, slug);
    }

    /**
     * Send email via JavaMailSender (SMTP).
     */
    @SuppressWarnings("null")
    private void sendEmail(String to, String subject, String htmlContent, String unsubToken) {
        // Replace template variables
        String personalizedHtml = htmlContent;
        if (unsubToken != null) {
            personalizedHtml = personalizedHtml.replace("{{UNSUB_TOKEN}}", unsubToken);
        }
        personalizedHtml = personalizedHtml.replace("{{BASE_URL}}", baseUrl);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(personalizedHtml, true);
            mailSender.send(message);
            log.debug("Newsletter email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send newsletter email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Email send failed: " + e.getMessage(), e);
        }
    }
}
