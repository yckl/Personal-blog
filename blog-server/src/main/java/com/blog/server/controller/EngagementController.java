package com.blog.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.blog.server.common.IpUtils;
import com.blog.server.common.Result;
import com.blog.server.entity.*;
import com.blog.server.exception.BusinessException;
import com.blog.server.mapper.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Engagement API — like, favorite, share for articles.
 * All public endpoints, IP-based dedup for likes/shares.
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class EngagementController {

    private final ArticleLikeMapper likeMapper;
    private final ArticleFavoriteMapper favoriteMapper;
    private final ArticleShareMapper shareMapper;
    private final ArticleMapper articleMapper;

    // ============ LIKE ============

    /**
     * Like/unlike an article (IP-based dedup).
     * Uses insert-first pattern to avoid race conditions.
     */
    @PostMapping("/article/{id}/like")
    @Transactional
    public Result<Map<String, Object>> likeArticle(@PathVariable Long id,
                                                    HttpServletRequest request) {
        assertArticleExists(id);
        String ip = IpUtils.getClientIp(request);

        // Check if already liked
        Long existing = likeMapper.selectCount(
                new LambdaQueryWrapper<ArticleLike>()
                        .eq(ArticleLike::getArticleId, id)
                        .eq(ArticleLike::getIpAddress, ip));
        if (existing > 0) {
            // Unlike
            likeMapper.delete(new LambdaQueryWrapper<ArticleLike>()
                    .eq(ArticleLike::getArticleId, id)
                    .eq(ArticleLike::getIpAddress, ip));
            articleMapper.update(null,
                    new LambdaUpdateWrapper<Article>()
                            .eq(Article::getId, id)
                            .setSql("like_count = GREATEST(like_count - 1, 0)"));
            Article article = articleMapper.selectById(id);
            return Result.ok(Map.of("liked", false, "likeCount", article != null ? article.getLikeCount() : 0));
        }

        // Like — try insert first, handle unique key violation
        try {
            ArticleLike like = new ArticleLike();
            like.setArticleId(id);
            like.setIpAddress(ip);
            likeMapper.insert(like);
            articleMapper.update(null,
                    new LambdaUpdateWrapper<Article>()
                            .eq(Article::getId, id)
                            .setSql("like_count = like_count + 1"));
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // Already liked (concurrent request) — skip count increment
        }

        Article article = articleMapper.selectById(id);
        return Result.ok(Map.of("liked", true, "likeCount", article != null ? article.getLikeCount() : 0));
    }

    /**
     * Check if current IP has liked the article.
     */
    @GetMapping("/article/{id}/like/status")
    public Result<Map<String, Object>> getLikeStatus(@PathVariable Long id,
                                                      HttpServletRequest request) {
        String ip = IpUtils.getClientIp(request);
        Long count = likeMapper.selectCount(
                new LambdaQueryWrapper<ArticleLike>()
                        .eq(ArticleLike::getArticleId, id)
                        .eq(ArticleLike::getIpAddress, ip));
        Article article = articleMapper.selectById(id);
        return Result.ok(Map.of(
                "liked", count > 0,
                "likeCount", article != null ? article.getLikeCount() : 0));
    }

    // ============ FAVORITE ============

    /**
     * Toggle favorite (device-id based since no user auth required).
     */
    @PostMapping("/article/{id}/favorite")
    public Result<Map<String, Object>> toggleFavorite(@PathVariable Long id,
                                                       @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
                                                       HttpServletRequest request) {
        assertArticleExists(id);
        long userId = deviceId != null ? Math.abs(deviceId.hashCode()) : Math.abs(IpUtils.getClientIp(request).hashCode());

        Long existing = favoriteMapper.selectCount(
                new LambdaQueryWrapper<ArticleFavorite>()
                        .eq(ArticleFavorite::getArticleId, id)
                        .eq(ArticleFavorite::getUserId, userId));

        if (existing > 0) {
            // Unfavorite
            favoriteMapper.delete(new LambdaQueryWrapper<ArticleFavorite>()
                    .eq(ArticleFavorite::getArticleId, id)
                    .eq(ArticleFavorite::getUserId, userId));
            return Result.ok(Map.of("favorited", false));
        }

        // Favorite
        ArticleFavorite fav = new ArticleFavorite();
        fav.setArticleId(id);
        fav.setUserId(userId);
        favoriteMapper.insert(fav);
        return Result.ok(Map.of("favorited", true));
    }

    /**
     * Check favorite status.
     */
    @GetMapping("/article/{id}/favorite/status")
    public Result<Map<String, Object>> getFavoriteStatus(@PathVariable Long id,
                                                          @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
                                                          HttpServletRequest request) {
        long userId = deviceId != null ? Math.abs(deviceId.hashCode()) : Math.abs(IpUtils.getClientIp(request).hashCode());
        Long count = favoriteMapper.selectCount(
                new LambdaQueryWrapper<ArticleFavorite>()
                        .eq(ArticleFavorite::getArticleId, id)
                        .eq(ArticleFavorite::getUserId, userId));
        return Result.ok(Map.of("favorited", count > 0));
    }

    /**
     * Get user's favorited articles list.
     */
    @GetMapping("/favorites")
    public Result<List<Map<String, Object>>> getFavorites(
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
            HttpServletRequest request) {
        long userId = deviceId != null ? Math.abs(deviceId.hashCode()) : Math.abs(IpUtils.getClientIp(request).hashCode());
        List<ArticleFavorite> favs = favoriteMapper.selectList(
                new LambdaQueryWrapper<ArticleFavorite>()
                        .eq(ArticleFavorite::getUserId, userId)
                        .orderByDesc(ArticleFavorite::getCreatedAt));
        if (favs.isEmpty()) return Result.ok(Collections.emptyList());

        Set<Long> ids = favs.stream().map(ArticleFavorite::getArticleId).collect(Collectors.toSet());
        List<Article> articles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .in(Article::getId, ids)
                        .eq(Article::getStatus, "PUBLISHED"));
        return Result.ok(articles.stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("title", a.getTitle());
            m.put("slug", a.getSlug());
            m.put("excerpt", a.getExcerpt());
            m.put("coverImage", a.getCoverImage());
            m.put("publishedAt", a.getPublishedAt());
            return m;
        }).collect(Collectors.toList()));
    }

    // ============ SHARE ============

    /**
     * Track a share event.
     */
    @PostMapping("/article/{id}/share")
    public Result<Void> trackShare(@PathVariable Long id,
                                    @RequestParam String platform,
                                    HttpServletRequest request) {
        assertArticleExists(id);
        ArticleShare share = new ArticleShare();
        share.setArticleId(id);
        share.setPlatform(platform);
        share.setIpAddress(IpUtils.getClientIp(request));
        shareMapper.insert(share);
        return Result.ok(null);
    }

    /**
     * Get share counts by platform for an article.
     */
    @GetMapping("/article/{id}/shares")
    public Result<Map<String, Long>> getShareCounts(@PathVariable Long id) {
        List<ArticleShare> shares = shareMapper.selectList(
                new LambdaQueryWrapper<ArticleShare>().eq(ArticleShare::getArticleId, id));
        Map<String, Long> counts = shares.stream()
                .collect(Collectors.groupingBy(ArticleShare::getPlatform, Collectors.counting()));
        return Result.ok(counts);
    }

    // ============ Helpers ============

    private void assertArticleExists(Long id) {
        if (articleMapper.selectCount(new LambdaQueryWrapper<Article>().eq(Article::getId, id)) == 0) {
            throw new BusinessException(404, "Article not found");
        }
    }
}
