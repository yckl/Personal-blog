package com.blog.server.controller;

import com.blog.server.common.PageResult;
import com.blog.server.common.Result;
import com.blog.server.dto.request.ArticleRequest;
import com.blog.server.dto.request.BatchArticleRequest;
import com.blog.server.dto.response.ArticleVO;
import com.blog.server.entity.SysUser;
import com.blog.server.service.AiService;
import com.blog.server.service.ArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.blog.server.annotation.RateLimit;

@RestController
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;
    private final AiService aiService;

    // ---- CRUD ----

    @PostMapping("/api/admin/article/create")
    public Result<Long> createArticle(@Valid @RequestBody ArticleRequest request,
                                      @AuthenticationPrincipal SysUser user) {
        return Result.ok(articleService.createArticle(request, user.getId()));
    }

    @PutMapping("/api/admin/article/update/{id}")
    public Result<Void> updateArticle(@PathVariable Long id,
                                      @Valid @RequestBody ArticleRequest request) {
        articleService.updateArticle(id, request);
        return Result.ok();
    }

    @DeleteMapping("/api/admin/article/{id}")
    public Result<Void> deleteArticle(@PathVariable Long id) {
        articleService.deleteArticle(id);
        return Result.ok();
    }

    @GetMapping("/api/admin/article/{id}")
    public Result<ArticleVO> getArticle(@PathVariable Long id) {
        return Result.ok(articleService.getArticleById(id));
    }

    @GetMapping("/api/admin/article/page")
    public Result<PageResult<ArticleVO>> listArticles(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort) {
        return Result.ok(articleService.listArticles(page, size, status, categoryId, tagId, keyword, sort));
    }

    // ---- Status transitions ----

    @PutMapping("/api/admin/article/{id}/draft")
    public Result<Void> unpublishArticle(@PathVariable Long id) {
        articleService.unpublishArticle(id);
        return Result.ok();
    }

    // ---- Batch operations ----

    @PostMapping("/api/admin/article/batch-delete")
    public Result<Void> batchDelete(@Valid @RequestBody BatchArticleRequest request) {
        articleService.batchDelete(request.getIds());
        return Result.ok();
    }

    @PostMapping("/api/admin/article/batch-top")
    public Result<Void> batchTop(@Valid @RequestBody BatchArticleRequest request) {
        articleService.batchSetTop(request.getIds(), true);
        return Result.ok();
    }

    @PostMapping("/api/admin/article/batch-archive")
    public Result<Void> batchArchive(@Valid @RequestBody BatchArticleRequest request) {
        articleService.batchArchive(request.getIds());
        return Result.ok();
    }

    @PostMapping("/api/admin/article/batch-publish")
    public Result<Void> batchPublish(@Valid @RequestBody BatchArticleRequest request) {
        articleService.batchPublish(request.getIds());
        return Result.ok();
    }

    @PostMapping("/api/admin/article/batch-unpublish")
    public Result<Void> batchUnpublish(@Valid @RequestBody BatchArticleRequest request) {
        articleService.batchUnpublish(request.getIds());
        return Result.ok();
    }

    // ---- Public API (kept for frontend blog) ----

    @RateLimit(maxRequests = 50, windowSeconds = 10)
    @GetMapping("/api/articles")
    public Result<PageResult<ArticleVO>> listPublicArticles(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort) {
        return Result.ok(articleService.listPublishedArticles(page, size, categoryId, tagId, keyword, sort));
    }

    @RateLimit(maxRequests = 30, windowSeconds = 10)
    @GetMapping("/api/articles/{id}")
    public Result<ArticleVO> getPublicArticle(@PathVariable Long id) {
        ArticleVO article = articleService.getArticleById(id);
        // Ensure only published articles are accessible via public API
        if (article == null || !"PUBLISHED".equals(article.getStatus())) {
            throw new com.blog.server.exception.BusinessException(404, "Article not found");
        }
        return Result.ok(article);
    }

    @RateLimit(maxRequests = 20, windowSeconds = 60)
    @GetMapping({"/api/articles/{id}/ai-summary", "/api/public/articles/{id}/ai-summary"})
    public Result<String> getArticleAiSummary(@PathVariable Long id) {
        ArticleVO article = articleService.getArticleById(id);
        if (article == null || !"PUBLISHED".equals(article.getStatus())) {
            throw new com.blog.server.exception.BusinessException(404, "Article not found");
        }
        String summaryJson = aiService.generateAbstract(article.getContentMd(), id, null);
        return Result.ok(summaryJson);
    }

    // ============ Recycle Bin ============

    @GetMapping("/api/admin/article/trash")
    public Result<com.blog.server.common.PageResult<ArticleVO>> listTrash(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return Result.ok(articleService.listDeletedArticles(page, size));
    }

    @PostMapping("/api/admin/article/{id}/restore")
    public Result<Void> restoreArticle(@PathVariable Long id) {
        articleService.restoreArticle(id);
        return Result.ok();
    }

    @DeleteMapping("/api/admin/article/{id}/permanent")
    public Result<Void> permanentDelete(@PathVariable Long id) {
        articleService.permanentDeleteArticle(id);
        return Result.ok();
    }
}
