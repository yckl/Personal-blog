package com.blog.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.server.common.PageResult;
import com.blog.server.common.Result;
import com.blog.server.entity.NewsletterTask;
import com.blog.server.mapper.NewsletterTaskMapper;
import com.blog.server.service.NewsletterService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Newsletter admin API — create, send, retry, stats, tracking.
 */
@RestController
@RequiredArgsConstructor
public class NewsletterController {

    private final NewsletterService newsletterService;
    private final NewsletterTaskMapper taskMapper;

    // ============ Admin Endpoints ============

    /**
     * Create newsletter from article (article → email).
     */
    @PostMapping("/api/admin/newsletter/from-article/{articleId}")
    public Result<NewsletterTask> createFromArticle(@PathVariable Long articleId) {
        return Result.ok(newsletterService.createFromArticle(articleId));
    }

    /**
     * Create manual newsletter.
     */
    @PostMapping("/api/admin/newsletter/create")
    public Result<NewsletterTask> createManual(@RequestBody CreateNewsletterRequest request) {
        return Result.ok(newsletterService.createManual(
                request.getSubject(), request.getContentHtml(), request.getAudienceType()));
    }

    /**
     * Schedule newsletter for later.
     */
    @PostMapping("/api/admin/newsletter/schedule/{id}")
    public Result<Void> schedule(@PathVariable Long id, @RequestParam String scheduledAt) {
        newsletterService.schedule(id, LocalDateTime.parse(scheduledAt));
        return Result.ok();
    }

    /**
     * Send newsletter immediately (async — returns immediately).
     */
    @PostMapping("/api/admin/newsletter/send/{id}")
    public Result<Map<String, String>> send(@PathVariable Long id) {
        newsletterService.sendAsync(id);
        return Result.ok(Map.of("message", "Newsletter sending started. Check stats for progress."));
    }

    /**
     * Retry failed sends.
     */
    @PostMapping("/api/admin/newsletter/retry/{id}")
    public Result<Map<String, String>> retry(@PathVariable Long id) {
        newsletterService.retryFailed(id);
        return Result.ok(Map.of("message", "Retrying failed sends..."));
    }

    /**
     * Get newsletter stats (delivery, open rate, click rate).
     */
    @GetMapping("/api/admin/newsletter/stats/{id}")
    public Result<Map<String, Object>> stats(@PathVariable Long id) {
        return Result.ok(newsletterService.getStats(id));
    }

    /**
     * List all newsletters with pagination.
     */
    @GetMapping("/api/admin/newsletter/page")
    public Result<PageResult<NewsletterTask>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String status) {
        Page<NewsletterTask> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<NewsletterTask> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) wrapper.eq(NewsletterTask::getStatus, status);
        wrapper.orderByDesc(NewsletterTask::getCreatedAt);
        Page<NewsletterTask> result = taskMapper.selectPage(pageParam, wrapper);
        return Result.ok(new PageResult<>(result.getRecords(), result.getTotal(),
                result.getCurrent(), result.getSize()));
    }

    /**
     * Delete a draft newsletter.
     */
    @DeleteMapping("/api/admin/newsletter/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        NewsletterTask task = taskMapper.selectById(id);
        if (task != null && "DRAFT".equals(task.getStatus())) {
            taskMapper.deleteById(id);
        }
        return Result.ok();
    }

    // ============ Public Tracking Endpoints ============

    /**
     * Tracking pixel — called when email is opened.
     * Returns 1x1 transparent GIF.
     */
    @GetMapping("/api/newsletter/track/open/{logId}")
    public byte[] trackOpen(@PathVariable Long logId) {
        newsletterService.trackOpen(logId);
        // 1x1 transparent GIF
        return new byte[]{
                0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0x01, 0x00,
                0x01, 0x00, (byte) 0x80, 0x00, 0x00, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                0x00, 0x00, 0x00, 0x21, (byte) 0xf9, 0x04, 0x01, 0x00, 0x00,
                0x00, 0x00, 0x2c, 0x00, 0x00, 0x00, 0x00, 0x01,
                0x00, 0x01, 0x00, 0x00, 0x02, 0x02, 0x44, 0x01,
                0x00, 0x3b
        };
    }

    /**
     * Click tracking — called via redirect link in email.
     */
    @GetMapping("/api/newsletter/track/click/{logId}")
    public String trackClick(@PathVariable Long logId, @RequestParam String url) {
        newsletterService.trackClick(logId);
        return "redirect:" + url;
    }

    @Data
    public static class CreateNewsletterRequest {
        private String subject;
        private String contentHtml;
        private String audienceType; // ALL, FREE, PREMIUM, TAG:xxx
    }
}
