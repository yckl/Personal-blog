package com.blog.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.server.common.Result;
import com.blog.server.entity.PushSubscription;
import com.blog.server.mapper.PushSubscriptionMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * V2: Push notification subscription management.
 * Frontend calls Web Push API → sends subscription to this endpoint.
 * Admin can trigger push to all subscribers.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class PushNotificationController {

    private final PushSubscriptionMapper subscriptionMapper;

    /**
     * Save a push subscription from the browser.
     */
    @PostMapping("/api/push/subscribe")
    public Result<Void> subscribe(@RequestBody PushSubscribeRequest req) {
        // Check if endpoint already exists
        Long existing = subscriptionMapper.selectCount(
                new LambdaQueryWrapper<PushSubscription>()
                        .eq(PushSubscription::getEndpoint, req.getEndpoint()));
        if (existing > 0) return Result.ok(); // Already subscribed

        PushSubscription sub = new PushSubscription();
        sub.setEndpoint(req.getEndpoint());
        sub.setP256dhKey(req.getP256dh());
        sub.setAuthKey(req.getAuth());
        sub.setMemberId(req.getMemberId());
        sub.setVisitorId(req.getVisitorId());
        subscriptionMapper.insert(sub);
        return Result.ok();
    }

    /**
     * Remove a push subscription.
     */
    @PostMapping("/api/push/unsubscribe")
    public Result<Void> unsubscribe(@RequestBody PushSubscribeRequest req) {
        subscriptionMapper.delete(
                new LambdaQueryWrapper<PushSubscription>()
                        .eq(PushSubscription::getEndpoint, req.getEndpoint()));
        return Result.ok();
    }

    /**
     * Admin: get subscription count.
     */
    @GetMapping("/api/admin/push/stats")
    public Result<Long> stats() {
        return Result.ok(subscriptionMapper.selectCount(null));
    }

    /**
     * Admin: list subscriptions.
     */
    @GetMapping("/api/admin/push/subscriptions")
    public Result<List<PushSubscription>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        var p = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<PushSubscription>(page, size);
        subscriptionMapper.selectPage(p, new LambdaQueryWrapper<PushSubscription>()
                .orderByDesc(PushSubscription::getCreatedAt));
        return Result.ok(p.getRecords());
    }

    /**
     * Admin: trigger push notification (stub — actual sending requires web-push library).
     * In production: use web-push library (e.g. webpush-java) with VAPID keys.
     */
    @PostMapping("/api/admin/push/send")
    public Result<Integer> sendPush(@RequestBody PushMessageRequest req) {
        List<PushSubscription> subs = subscriptionMapper.selectList(null);
        log.info("[Push] Sending '{}' to {} subscribers", req.getTitle(), subs.size());
        // FUTURE: Integrate web-push library (e.g. webpush-java) with VAPID keys to send actual notifications.
        // For each subscription: webPush.send(sub.endpoint, sub.p256dhKey, sub.authKey, payload)
        return Result.ok(subs.size());
    }

    @Data
    public static class PushSubscribeRequest {
        private String endpoint;
        private String p256dh;
        private String auth;
        private Long memberId;
        private String visitorId;
    }

    @Data
    public static class PushMessageRequest {
        private String title;
        private String body;
        private String url;
    }
}
