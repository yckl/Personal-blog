package com.blog.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.server.entity.*;
import com.blog.server.exception.BusinessException;
import com.blog.server.mapper.*;
import com.blog.server.service.PublishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublishServiceImpl implements PublishService {

    private final ArticleMapper articleMapper;
    private final ArticleContentMapper contentMapper;
    private final SubscriberMapper subscriberMapper;
    private final NewsletterTaskMapper newsletterTaskMapper;
    private final PreviewTokenMapper previewTokenMapper;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@blog.com}")
    private String fromEmail;

    @Value("${app.site-url:http://localhost:3000}")
    private String siteUrl;

    @Override
    @Transactional
    public Map<String, Object> publishArticle(Long articleId, boolean sendNewsletter, String visibility, Long userId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) throw new BusinessException(404, "Article not found");

        // ---- Validation ----
        validateForPublish(article, articleId);

        // ---- Set status & visibility ----
        String status = visibility != null && ("MEMBER_ONLY".equals(visibility) || "PRIVATE".equals(visibility))
                ? visibility : "PUBLISHED";
        article.setStatus(status);
        article.setPublishedAt(LocalDateTime.now());
        articleMapper.updateById(article);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("articleId", articleId);
        result.put("status", status);
        result.put("publishedAt", article.getPublishedAt());

        // ---- Newsletter ----
        if (sendNewsletter && "PUBLISHED".equals(status)) {
            Long taskId = createNewsletterTask(article, userId);
            result.put("newsletterTaskId", taskId);
            result.put("newsletterStatus", "SENDING");
            // Fire async
            sendNewsletterAsync(taskId, article);
        }

        return result;
    }

    @Override
    @Transactional
    public void scheduleArticle(Long articleId, String scheduledAt, boolean sendNewsletter, String visibility, Long userId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) throw new BusinessException(404, "Article not found");

        validateForPublish(article, articleId);

        LocalDateTime scheduledTime = LocalDateTime.parse(scheduledAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        if (scheduledTime.isBefore(LocalDateTime.now())) {
            throw new BusinessException("Scheduled time must be in the future");
        }

        article.setStatus("SCHEDULED");
        article.setScheduledAt(scheduledTime);
        articleMapper.updateById(article);

        // If newsletter desired, create task in PENDING state — scheduler will trigger it
        if (sendNewsletter) {
            NewsletterTask task = new NewsletterTask();
            task.setArticleId(articleId);
            task.setStatus("PENDING");
            task.setSubject("New Post: " + article.getTitle());
            task.setCreatedBy(userId);
            task.setTotalCount(0);
            task.setSentCount(0);
            task.setFailedCount(0);
            newsletterTaskMapper.insert(task);
        }
    }

    @Override
    @Transactional
    public Map<String, Object> generatePreviewLink(Long articleId, Long userId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) throw new BusinessException(404, "Article not found");

        String token = UUID.randomUUID().toString().replace("-", "");

        PreviewToken pt = new PreviewToken();
        pt.setArticleId(articleId);
        pt.setToken(token);
        pt.setExpiresAt(LocalDateTime.now().plusHours(24)); // 24h expiry
        pt.setCreatedBy(userId);
        previewTokenMapper.insert(pt);

        String previewUrl = siteUrl + "/preview/" + token;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("previewUrl", previewUrl);
        result.put("token", token);
        result.put("expiresAt", pt.getExpiresAt());
        return result;
    }

    @Override
    public Map<String, Object> getPreviewByToken(String token) {
        PreviewToken pt = previewTokenMapper.selectOne(
                new LambdaQueryWrapper<PreviewToken>().eq(PreviewToken::getToken, token));
        if (pt == null) throw new BusinessException(404, "Preview link not found");
        if (pt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(410, "Preview link has expired");
        }

        Article article = articleMapper.selectById(pt.getArticleId());
        ArticleContent content = contentMapper.selectOne(
                new LambdaQueryWrapper<ArticleContent>().eq(ArticleContent::getArticleId, pt.getArticleId()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("article", article);
        result.put("content", content);
        result.put("expiresAt", pt.getExpiresAt());
        return result;
    }

    // ---- Private helpers ----

    private void validateForPublish(Article article, Long articleId) {
        if (article.getTitle() == null || article.getTitle().trim().isEmpty()) {
            throw new BusinessException("Title is required for publishing");
        }
        if (article.getSlug() == null || article.getSlug().trim().isEmpty()) {
            throw new BusinessException("Slug is required for publishing");
        }
        // Check slug uniqueness
        Article existing = articleMapper.selectOne(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getSlug, article.getSlug())
                        .ne(Article::getId, articleId));
        if (existing != null) {
            throw new BusinessException("Slug '" + article.getSlug() + "' is already in use");
        }
        // Check content
        ArticleContent content = contentMapper.selectOne(
                new LambdaQueryWrapper<ArticleContent>().eq(ArticleContent::getArticleId, articleId));
        if (content == null || content.getContentMd() == null || content.getContentMd().trim().isEmpty()) {
            throw new BusinessException("Article content is required for publishing");
        }
    }

    private Long createNewsletterTask(Article article, Long userId) {
        List<Subscriber> activeSubscribers = subscriberMapper.selectList(
                new LambdaQueryWrapper<Subscriber>().eq(Subscriber::getStatus, "CONFIRMED"));

        NewsletterTask task = new NewsletterTask();
        task.setArticleId(article.getId());
        task.setStatus("SENDING");
        task.setSubject("New Post: " + article.getTitle());
        task.setTotalCount(activeSubscribers.size());
        task.setSentCount(0);
        task.setFailedCount(0);
        task.setCreatedBy(userId);
        newsletterTaskMapper.insert(task);
        return task.getId();
    }

    @Async
    public void sendNewsletterAsync(Long taskId, Article article) {
        try {
            List<Subscriber> subscribers = subscriberMapper.selectList(
                    new LambdaQueryWrapper<Subscriber>().eq(Subscriber::getStatus, "CONFIRMED"));

            ArticleContent content = contentMapper.selectOne(
                    new LambdaQueryWrapper<ArticleContent>().eq(ArticleContent::getArticleId, article.getId()));

            NewsletterTask task = newsletterTaskMapper.selectById(taskId);
            int sent = 0, failed = 0;

            for (Subscriber sub : subscribers) {
                try {
                    sendEmail(sub.getEmail(), sub.getName(), article, content);
                    sent++;
                } catch (Exception e) {
                    log.warn("Failed to send email to {}: {}", sub.getEmail(), e.getMessage());
                    failed++;
                }
            }

            task.setSentCount(sent);
            task.setFailedCount(failed);
            task.setStatus(failed == 0 ? "SENT" : (sent > 0 ? "SENT" : "FAILED"));
            task.setSentAt(LocalDateTime.now());
            newsletterTaskMapper.updateById(task);

        } catch (Exception e) {
            log.error("Newsletter task {} failed: {}", taskId, e.getMessage());
            NewsletterTask task = newsletterTaskMapper.selectById(taskId);
            if (task != null) {
                task.setStatus("FAILED");
                task.setSentAt(LocalDateTime.now());
                newsletterTaskMapper.updateById(task);
            }
        }
    }

    @SuppressWarnings("null")
    private void sendEmail(String toEmail, String name, Article article, ArticleContent content) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(toEmail);
        helper.setSubject("New Post: " + article.getTitle());

        String articleUrl = siteUrl + "/blog/" + article.getSlug();
        String excerpt = article.getExcerpt() != null ? article.getExcerpt() : "";
        String subscriberName = name != null ? name : "Reader";

        // Email HTML template
        String html = """
                <div style="max-width:600px;margin:0 auto;font-family:system-ui,-apple-system,sans-serif;color:#333;">
                  <h2 style="color:#1a1a2e;">%s</h2>
                  <p>Hi %s,</p>
                  <p>A new article has been published on our blog:</p>
                  <div style="background:#f8f9fa;padding:20px;border-radius:8px;margin:16px 0;">
                    <h3 style="margin-top:0;"><a href="%s" style="color:#409eff;text-decoration:none;">%s</a></h3>
                    <p style="color:#666;">%s</p>
                  </div>
                  <a href="%s" style="display:inline-block;padding:12px 24px;background:#409eff;color:white;text-decoration:none;border-radius:6px;">Read Full Article</a>
                  <hr style="margin:24px 0;border:none;border-top:1px solid #eee;">
                  <p style="font-size:12px;color:#999;">You received this because you subscribed. <a href="%s/unsubscribe?email=%s">Unsubscribe</a></p>
                </div>
                """.formatted(article.getTitle(), subscriberName, articleUrl, article.getTitle(), excerpt, articleUrl, siteUrl, toEmail);

        helper.setText(html, true);
        mailSender.send(message);
    }
}
