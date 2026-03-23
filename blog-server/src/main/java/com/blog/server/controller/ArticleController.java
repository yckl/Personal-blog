package com.blog.server.controller;

import com.blog.server.common.PageResult;
import com.blog.server.common.Result;
import com.blog.server.dto.request.ArticleRequest;
import com.blog.server.dto.request.BatchArticleRequest;
import com.blog.server.dto.response.ArticleVO;
import com.blog.server.entity.SysUser;
import com.blog.server.service.ArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

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
            @RequestParam(required = false) String keyword) {
        return Result.ok(articleService.listArticles(page, size, status, categoryId, tagId, keyword));
    }

    // ---- Status transitions ----

    @Deprecated
    @PostMapping("/api/admin/article/publish-basic/{id}")
    public Result<Void> publishArticle(@PathVariable Long id) {
        articleService.publishArticle(id);
        return Result.ok();
    }

    @Deprecated
    @PostMapping("/api/admin/article/schedule-basic/{id}")
    public Result<Void> scheduleArticle(@PathVariable Long id,
                                        @RequestParam LocalDateTime scheduledAt) {
        articleService.scheduleArticle(id, scheduledAt);
        return Result.ok();
    }

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

    @GetMapping("/api/articles")
    public Result<PageResult<ArticleVO>> listPublicArticles(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String keyword) {
        return Result.ok(articleService.listPublishedArticles(page, size, categoryId, tagId, keyword));
    }

    @GetMapping("/api/articles/{id}")
    public Result<ArticleVO> getPublicArticle(@PathVariable Long id) {
        return Result.ok(articleService.getArticleById(id));
    }
}
