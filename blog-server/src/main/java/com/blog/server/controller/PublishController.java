package com.blog.server.controller;

import com.blog.server.common.Result;
import com.blog.server.entity.SysUser;
import com.blog.server.service.PublishService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PublishController {

    private final PublishService publishService;

    /** Full publish with validation, visibility, and optional newsletter */
    @PostMapping("/api/admin/article/publish/{id}")
    public Result<Map<String, Object>> publish(@PathVariable Long id,
                                                @RequestBody(required = false) PublishRequest req,
                                                @AuthenticationPrincipal SysUser user) {
        PublishRequest r = req != null ? req : new PublishRequest();
        return Result.ok(publishService.publishArticle(id, r.isSendNewsletter(), r.getVisibility(), user.getId()));
    }

    /** Schedule for future publication */
    @PostMapping("/api/admin/article/schedule/{id}")
    public Result<Void> schedule(@PathVariable Long id,
                                  @RequestBody ScheduleRequest req,
                                  @AuthenticationPrincipal SysUser user) {
        publishService.scheduleArticle(id, req.getScheduledAt(), req.isSendNewsletter(), req.getVisibility(), user.getId());
        return Result.ok();
    }

    /** Generate time-limited preview link (24h) */
    @PostMapping("/api/admin/article/preview/{id}")
    public Result<Map<String, Object>> preview(@PathVariable Long id,
                                                @AuthenticationPrincipal SysUser user) {
        return Result.ok(publishService.generatePreviewLink(id, user.getId()));
    }

    /** Public endpoint to view a preview by token */
    @GetMapping("/api/public/preview/{token}")
    public Result<Map<String, Object>> getPreview(@PathVariable String token) {
        return Result.ok(publishService.getPreviewByToken(token));
    }

    @Data
    public static class PublishRequest {
        private boolean sendNewsletter;
        private String visibility; // PUBLISHED, PRIVATE, MEMBER_ONLY
    }

    @Data
    public static class ScheduleRequest {
        private String scheduledAt;
        private boolean sendNewsletter;
        private String visibility;
    }
}
