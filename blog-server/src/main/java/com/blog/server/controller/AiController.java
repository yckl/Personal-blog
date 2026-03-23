package com.blog.server.controller;

import com.blog.server.common.Result;
import com.blog.server.entity.SysUser;
import com.blog.server.service.AiService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/generate-title")
    public Result<String> generateTitle(@RequestBody AiRequest req,
                                        @AuthenticationPrincipal SysUser user) {
        return Result.ok(aiService.generateTitle(req.getContent(), req.getArticleId(), user.getId()));
    }

    @PostMapping("/generate-summary")
    public Result<String> generateSummary(@RequestBody AiRequest req,
                                          @AuthenticationPrincipal SysUser user) {
        return Result.ok(aiService.generateSummary(req.getContent(), req.getArticleId(), user.getId()));
    }

    @PostMapping("/generate-seo-title")
    public Result<String> generateSeoTitle(@RequestBody AiRequest req,
                                           @AuthenticationPrincipal SysUser user) {
        return Result.ok(aiService.generateSeoTitle(req.getContent(), req.getCurrentTitle(), req.getArticleId(), user.getId()));
    }

    @PostMapping("/generate-meta-description")
    public Result<String> generateMetaDescription(@RequestBody AiRequest req,
                                                   @AuthenticationPrincipal SysUser user) {
        return Result.ok(aiService.generateMetaDescription(req.getContent(), req.getArticleId(), user.getId()));
    }

    @PostMapping("/generate-outline")
    public Result<String> generateOutline(@RequestBody AiRequest req,
                                          @AuthenticationPrincipal SysUser user) {
        return Result.ok(aiService.generateOutline(req.getContent(), req.getArticleId(), user.getId()));
    }

    @PostMapping("/recommend-tags")
    public Result<String> recommendTags(@RequestBody AiRequest req,
                                        @AuthenticationPrincipal SysUser user) {
        return Result.ok(aiService.recommendTags(req.getContent(), req.getArticleId(), user.getId()));
    }

    @PostMapping("/generate-newsletter")
    public Result<String> generateNewsletter(@RequestBody AiRequest req,
                                             @AuthenticationPrincipal SysUser user) {
        return Result.ok(aiService.generateNewsletter(req.getContent(), req.getArticleId(), user.getId()));
    }

    @PostMapping("/recommend-internal-links")
    public Result<String> recommendInternalLinks(@RequestBody AiRequest req,
                                                  @AuthenticationPrincipal SysUser user) {
        return Result.ok(aiService.recommendInternalLinks(req.getContent(), req.getArticleId(), user.getId()));
    }

    @Data
    public static class AiRequest {
        private String content;
        private String currentTitle;
        private Long articleId;
    }
}
