package com.blog.server.controller;

import com.blog.server.common.PageResult;
import com.blog.server.common.Result;
import com.blog.server.entity.Subscriber;
import com.blog.server.exception.BusinessException;
import com.blog.server.mapper.SubscriberMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import com.blog.server.entity.WelcomeSequenceTemplate;
import com.blog.server.service.WelcomeSequenceService;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Subscription system with double opt-in confirmation.
 *
 * Flow: Subscribe → PENDING → Confirm email → CONFIRMED
 * Unsubscribe: Token-based link in every email
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class SubscriberController {

    private final SubscriberMapper subscriberMapper;
    private final WelcomeSequenceService welcomeSequenceService;

    @Value("${app.base-url:http://localhost:5173}")
    private String baseUrl;

    // ============ Public Endpoints ============

    /**
     * Subscribe — creates PENDING record and returns confirm token.
     * In production, send confirmation email with the token link.
     */
    @PostMapping("/api/subscribe")
    public Result<Map<String, Object>> subscribe(@RequestBody SubscribeRequest request) {
        if (!StringUtils.hasText(request.getEmail())) {
            throw new BusinessException("Email is required");
        }
        String email = request.getEmail().trim().toLowerCase();

        // Check existing
        Subscriber existing = subscriberMapper.selectOne(
                new LambdaQueryWrapper<Subscriber>().eq(Subscriber::getEmail, email));

        if (existing != null) {
            if ("CONFIRMED".equals(existing.getStatus())) {
                return Result.ok(Map.of("message", "You are already subscribed!", "alreadySubscribed", true));
            }
            if ("PENDING".equals(existing.getStatus())) {
                // Resend confirmation — return token
                return Result.ok(Map.of(
                        "message", "Confirmation email resent. Please check your inbox.",
                        "confirmToken", existing.getConfirmToken(),
                        "confirmUrl", baseUrl + "/subscribe/confirm?token=" + existing.getConfirmToken()
                ));
            }
            // UNSUBSCRIBED — re-subscribe with new token
            String token = generateToken();
            existing.setStatus("PENDING");
            existing.setConfirmToken(token);
            existing.setSubscribedAt(LocalDateTime.now());
            existing.setUnsubscribedAt(null);
            existing.setSource(request.getSource() != null ? request.getSource() : "website");
            if (StringUtils.hasText(request.getName())) existing.setName(request.getName().trim());
            if (StringUtils.hasText(request.getTags())) existing.setTags(request.getTags());
            subscriberMapper.updateById(existing);
            return Result.ok(Map.of(
                    "message", "Please confirm your email to resubscribe.",
                    "confirmToken", token,
                    "confirmUrl", baseUrl + "/subscribe/confirm?token=" + token
            ));
        }

        // New subscriber
        String token = generateToken();
        Subscriber subscriber = new Subscriber();
        subscriber.setEmail(email);
        subscriber.setName(StringUtils.hasText(request.getName()) ? request.getName().trim() : null);
        subscriber.setStatus("PENDING");
        subscriber.setConfirmToken(token);
        subscriber.setSource(request.getSource() != null ? request.getSource() : "website");
        subscriber.setTags(request.getTags());
        subscriber.setSubscribedAt(LocalDateTime.now());
        subscriberMapper.insert(subscriber);

        log.info("New subscriber: {} from source: {}", email, subscriber.getSource());

        // In production, send email here. For now return the confirm URL.
        return Result.ok(Map.of(
                "message", "Please check your email to confirm your subscription.",
                "confirmToken", token,
                "confirmUrl", baseUrl + "/subscribe/confirm?token=" + token
        ));
    }

    /**
     * Confirm subscription — activates the subscriber.
     */
    @GetMapping("/api/subscribe/confirm")
    public Result<Map<String, String>> confirmSubscription(@RequestParam String token) {
        Subscriber subscriber = subscriberMapper.selectOne(
                new LambdaQueryWrapper<Subscriber>().eq(Subscriber::getConfirmToken, token));

        if (subscriber == null) {
            throw new BusinessException(404, "Invalid or expired confirmation link.");
        }
        if ("CONFIRMED".equals(subscriber.getStatus())) {
            return Result.ok(Map.of("message", "Your subscription is already confirmed!"));
        }

        subscriber.setStatus("CONFIRMED");
        subscriber.setConfirmedAt(LocalDateTime.now());
        subscriberMapper.updateById(subscriber);

        // Enqueue welcome sequence
        welcomeSequenceService.enqueueForSubscriber(subscriber);

        log.info("Subscriber confirmed: {}", subscriber.getEmail());
        return Result.ok(Map.of("message", "🎉 Subscription confirmed! Welcome aboard."));
    }

    /**
     * Unsubscribe via token (embedded in every email).
     */
    @GetMapping("/api/unsubscribe")
    public Result<Map<String, String>> unsubscribe(@RequestParam String token) {
        Subscriber subscriber = subscriberMapper.selectOne(
                new LambdaQueryWrapper<Subscriber>().eq(Subscriber::getConfirmToken, token));

        if (subscriber == null) {
            throw new BusinessException(404, "Invalid unsubscribe link.");
        }
        if ("UNSUBSCRIBED".equals(subscriber.getStatus())) {
            return Result.ok(Map.of("message", "You have already unsubscribed."));
        }

        subscriber.setStatus("UNSUBSCRIBED");
        subscriber.setUnsubscribedAt(LocalDateTime.now());
        subscriberMapper.updateById(subscriber);

        log.info("Subscriber unsubscribed: {}", subscriber.getEmail());
        return Result.ok(Map.of("message", "You have been unsubscribed. We're sorry to see you go."));
    }

    /**
     * Update subscription preferences (tags).
     */
    @PostMapping("/api/subscribe/preferences")
    public Result<Void> updatePreferences(@RequestParam String token, @RequestBody PreferencesRequest request) {
        Subscriber subscriber = subscriberMapper.selectOne(
                new LambdaQueryWrapper<Subscriber>().eq(Subscriber::getConfirmToken, token));
        if (subscriber == null) {
            throw new BusinessException(404, "Invalid token.");
        }
        subscriber.setTags(request.getTags());
        if (StringUtils.hasText(request.getName())) subscriber.setName(request.getName());
        subscriberMapper.updateById(subscriber);
        return Result.ok();
    }

    // ============ Admin Endpoints ============

    /**
     * List all subscribers with filtering.
     */
    @GetMapping("/api/admin/subscribers")
    public Result<PageResult<Subscriber>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String source) {
        Page<Subscriber> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Subscriber> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) wrapper.eq(Subscriber::getStatus, status);
        if (StringUtils.hasText(source)) wrapper.eq(Subscriber::getSource, source);
        wrapper.orderByDesc(Subscriber::getSubscribedAt);
        Page<Subscriber> result = subscriberMapper.selectPage(pageParam, wrapper);
        return Result.ok(new PageResult<>(result.getRecords(), result.getTotal(),
                result.getCurrent(), result.getSize()));
    }

    /**
     * Get subscriber stats for admin dashboard.
     */
    @GetMapping("/api/admin/subscribers/stats")
    public Result<Map<String, Object>> stats() {
        Long total = subscriberMapper.selectCount(null);
        Long confirmed = subscriberMapper.selectCount(
                new LambdaQueryWrapper<Subscriber>().eq(Subscriber::getStatus, "CONFIRMED"));
        Long pending = subscriberMapper.selectCount(
                new LambdaQueryWrapper<Subscriber>().eq(Subscriber::getStatus, "PENDING"));
        Long unsubscribed = subscriberMapper.selectCount(
                new LambdaQueryWrapper<Subscriber>().eq(Subscriber::getStatus, "UNSUBSCRIBED"));

        // Source breakdown
        List<Subscriber> all = subscriberMapper.selectList(
                new LambdaQueryWrapper<Subscriber>().eq(Subscriber::getStatus, "CONFIRMED"));
        Map<String, Long> bySource = new LinkedHashMap<>();
        for (Subscriber s : all) {
            String src = s.getSource() != null ? s.getSource() : "unknown";
            bySource.merge(src, 1L, (a, b) -> a + b);
        }

        return Result.ok(Map.of(
                "total", total,
                "confirmed", confirmed,
                "pending", pending,
                "unsubscribed", unsubscribed,
                "bySource", bySource
        ));
    }

    /**
     * Admin: delete subscriber.
     */
    @DeleteMapping("/api/admin/subscribers/{id}")
    public Result<Void> deleteSubscriber(@PathVariable Long id) {
        subscriberMapper.deleteById(id);
        return Result.ok();
    }

    // ============ Welcome Sequence Admin ============

    @GetMapping("/api/admin/welcome-sequence/templates")
    public Result<List<WelcomeSequenceTemplate>> listWelcomeTemplates() {
        return Result.ok(welcomeSequenceService.listTemplates());
    }

    @PutMapping("/api/admin/welcome-sequence/templates/{id}")
    public Result<Void> updateWelcomeTemplate(@PathVariable Long id,
                                               @RequestBody WelcomeSequenceTemplate template) {
        template.setId(id);
        welcomeSequenceService.updateTemplate(template);
        return Result.ok();
    }

    @GetMapping("/api/admin/welcome-sequence/status/{subscriberId}")
    public Result<List<Map<String, Object>>> getWelcomeSequenceStatus(@PathVariable Long subscriberId) {
        return Result.ok(welcomeSequenceService.getSequenceStatus(subscriberId));
    }

    // ============ Helpers ============

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "")
                + Long.toHexString(System.currentTimeMillis());
    }

    @Data
    public static class SubscribeRequest {
        private String email;
        private String name;
        private String source; // website, article, footer, popup
        private String tags;   // comma-separated preference tags
    }

    @Data
    public static class PreferencesRequest {
        private String name;
        private String tags;
    }
}
