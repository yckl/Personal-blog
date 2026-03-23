package com.blog.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.server.common.Result;
import com.blog.server.entity.ArticleAuthor;
import com.blog.server.mapper.ArticleAuthorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * V3: Multi-author support — manage article co-authors.
 */
@RestController
@RequiredArgsConstructor
public class AuthorController {

    private final ArticleAuthorMapper authorMapper;

    /**
     * Get all authors for an article.
     */
    @GetMapping("/api/public/articles/{articleId}/authors")
    public Result<List<ArticleAuthor>> getArticleAuthors(@PathVariable Long articleId) {
        return Result.ok(authorMapper.selectList(
                new LambdaQueryWrapper<ArticleAuthor>()
                        .eq(ArticleAuthor::getArticleId, articleId)
                        .orderByAsc(ArticleAuthor::getCreatedAt)));
    }

    /**
     * Get all articles by an author.
     */
    @GetMapping("/api/public/authors/{userId}/articles")
    public Result<List<ArticleAuthor>> getAuthorArticles(@PathVariable Long userId) {
        return Result.ok(authorMapper.selectList(
                new LambdaQueryWrapper<ArticleAuthor>()
                        .eq(ArticleAuthor::getUserId, userId)
                        .orderByDesc(ArticleAuthor::getCreatedAt)));
    }

    // ============ Admin ============

    /**
     * Add a co-author to an article.
     */
    @PostMapping("/api/admin/articles/{articleId}/authors")
    public Result<ArticleAuthor> addAuthor(
            @PathVariable Long articleId,
            @RequestBody ArticleAuthor author) {
        // Check if already exists
        Long existing = authorMapper.selectCount(
                new LambdaQueryWrapper<ArticleAuthor>()
                        .eq(ArticleAuthor::getArticleId, articleId)
                        .eq(ArticleAuthor::getUserId, author.getUserId()));
        if (existing > 0) return Result.fail("Author already assigned to this article");

        author.setArticleId(articleId);
        if (author.getRole() == null) author.setRole("CO_AUTHOR");
        authorMapper.insert(author);
        return Result.ok(author);
    }

    /**
     * Remove a co-author from an article.
     */
    @DeleteMapping("/api/admin/articles/{articleId}/authors/{userId}")
    public Result<Void> removeAuthor(@PathVariable Long articleId, @PathVariable Long userId) {
        authorMapper.delete(
                new LambdaQueryWrapper<ArticleAuthor>()
                        .eq(ArticleAuthor::getArticleId, articleId)
                        .eq(ArticleAuthor::getUserId, userId));
        return Result.ok();
    }

    /**
     * Update author role (PRIMARY, CO_AUTHOR, REVIEWER).
     */
    @PutMapping("/api/admin/articles/{articleId}/authors/{userId}")
    public Result<Void> updateRole(
            @PathVariable Long articleId,
            @PathVariable Long userId,
            @RequestBody Map<String, String> body) {
        ArticleAuthor author = authorMapper.selectOne(
                new LambdaQueryWrapper<ArticleAuthor>()
                        .eq(ArticleAuthor::getArticleId, articleId)
                        .eq(ArticleAuthor::getUserId, userId));
        if (author == null) return Result.fail("Author not found");

        author.setRole(body.getOrDefault("role", "CO_AUTHOR"));
        authorMapper.updateById(author);
        return Result.ok();
    }
}
