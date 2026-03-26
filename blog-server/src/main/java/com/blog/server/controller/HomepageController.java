package com.blog.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.server.common.Result;
import com.blog.server.entity.*;
import com.blog.server.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Aggregated public homepage API — single call returns all homepage data.
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class HomepageController {

    private final ArticleMapper articleMapper;
    private final ArticleTagRelMapper tagRelMapper;
    private final ArticleTagMapper tagMapper;
    private final ArticleCategoryMapper categoryMapper;
    private final ArticleSeriesMapper seriesMapper;
    private final ArticleSeriesRelMapper seriesRelMapper;
    private final SiteConfigMapper siteConfigMapper;

    @GetMapping("/homepage")
    public Result<Map<String, Object>> getHomepageData() {
        Map<String, Object> result = new LinkedHashMap<>();

        // --- Site Info from config ---
        List<SiteConfig> configs = siteConfigMapper.selectList(new LambdaQueryWrapper<>());
        Map<String, String> siteInfo = new LinkedHashMap<>();
        for (SiteConfig c : configs) {
            siteInfo.put(c.getConfigKey(), c.getConfigValue());
        }
        result.put("siteInfo", siteInfo);

        // --- Hero config ---
        Map<String, String> hero = new LinkedHashMap<>();
        hero.put("badge", siteInfo.getOrDefault("hero_badge", "✨ 个人博客"));
        hero.put("title", siteInfo.getOrDefault("hero_title", "思考、技术与值得分享的想法"));
        hero.put("subtitle", siteInfo.getOrDefault("hero_subtitle", "关注技术、产品、设计与长期主义，记录构建有价值事物的旅程。"));
        hero.put("ctaText", siteInfo.getOrDefault("hero_cta_text", "阅读文章 →"));
        hero.put("ctaLink", siteInfo.getOrDefault("hero_cta_link", "/articles"));
        result.put("hero", hero);

        // --- Preload categories map ---
        List<ArticleCategory> cats = categoryMapper.selectList(new LambdaQueryWrapper<>());
        Map<Long, String> catNameMap = cats.stream()
                .collect(Collectors.toMap(ArticleCategory::getId, ArticleCategory::getName, (a, b) -> a));

        // --- Preload tags map ---
        List<ArticleTag> allTags = tagMapper.selectList(new LambdaQueryWrapper<>());
        Map<Long, ArticleTag> tagMap = allTags.stream()
                .collect(Collectors.toMap(ArticleTag::getId, t -> t, (a, b) -> a));
        List<ArticleTagRel> allRels = tagRelMapper.selectList(new LambdaQueryWrapper<>());
        Map<Long, List<ArticleTag>> articleTagsMap = new HashMap<>();
        for (ArticleTagRel rel : allRels) {
            ArticleTag tag = tagMap.get(rel.getTagId());
            if (tag != null) {
                articleTagsMap.computeIfAbsent(rel.getArticleId(), k -> new ArrayList<>()).add(tag);
            }
        }

        // --- Top / pinned (isTop = true, LIMIT 6) ---
        List<Article> topArticles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getStatus, "PUBLISHED")
                        .eq(Article::getIsTop, true)
                        .orderByDesc(Article::getPublishedAt)
                        .last("LIMIT 6"));
        List<Map<String, Object>> top = topArticles.stream()
                .map(a -> toArticleCard(a, catNameMap, articleTagsMap))
                .collect(Collectors.toList());
        result.put("topArticles", top);

        // --- Featured (isFeatured = true, LIMIT 6) ---
        List<Article> featuredArticles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getStatus, "PUBLISHED")
                        .eq(Article::getIsFeatured, true)
                        .orderByDesc(Article::getPublishedAt)
                        .last("LIMIT 6"));
        List<Map<String, Object>> featured = featuredArticles.stream()
                .map(a -> toArticleCard(a, catNameMap, articleTagsMap))
                .collect(Collectors.toList());
        result.put("featuredArticles", featured);

        // --- Collect IDs to exclude from latest ---
        Set<Long> excludedLatestIds = new LinkedHashSet<>();
        topArticles.forEach(a -> excludedLatestIds.add(a.getId()));
        featuredArticles.forEach(a -> excludedLatestIds.add(a.getId()));

        // --- Latest (newest 10, excluding top/featured) ---
        LambdaQueryWrapper<Article> latestQuery = new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, "PUBLISHED")
                .orderByDesc(Article::getPublishedAt)
                .last("LIMIT 10");
        if (!excludedLatestIds.isEmpty()) {
            latestQuery.notIn(Article::getId, excludedLatestIds);
        }
        List<Map<String, Object>> latest = articleMapper.selectList(latestQuery).stream()
                .map(a -> toArticleCard(a, catNameMap, articleTagsMap))
                .collect(Collectors.toList());
        result.put("latestArticles", latest);

        // --- Popular (by view count, top 6) ---
        List<Map<String, Object>> popular = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getStatus, "PUBLISHED")
                        .orderByDesc(Article::getViewCount)
                        .last("LIMIT 6")).stream()
                .map(a -> toArticleCard(a, catNameMap, articleTagsMap))
                .collect(Collectors.toList());
        result.put("popularArticles", popular);

        // --- Series (with article count) ---
        List<ArticleSeries> seriesList = seriesMapper.selectList(
                new LambdaQueryWrapper<ArticleSeries>().orderByAsc(ArticleSeries::getSortOrder));
        List<ArticleSeriesRel> seriesRels = seriesRelMapper.selectList(new LambdaQueryWrapper<>());
        Map<Long, Long> seriesCountMap = seriesRels.stream()
                .collect(Collectors.groupingBy(ArticleSeriesRel::getSeriesId, Collectors.counting()));

        List<Map<String, Object>> series = seriesList.stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("name", s.getName());
            m.put("slug", s.getSlug());
            m.put("description", s.getDescription());
            m.put("coverImage", s.getCoverImage());
            m.put("articleCount", seriesCountMap.getOrDefault(s.getId(), 0L));
            return m;
        }).collect(Collectors.toList());
        result.put("seriesList", series);

        // --- Stats (use COUNT query instead of loading all articles) ---
        Long totalPublished = articleMapper.selectCount(
                new LambdaQueryWrapper<Article>().eq(Article::getStatus, "PUBLISHED"));
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalArticles", totalPublished);
        stats.put("totalViews", 0); // Could use a SUM query if needed
        result.put("stats", stats);

        return Result.ok(result);
    }

    private Map<String, Object> toArticleCard(Article a, Map<Long, String> catNameMap, Map<Long, List<ArticleTag>> articleTagsMap) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("title", a.getTitle());
        m.put("slug", a.getSlug());
        m.put("excerpt", a.getExcerpt());
        m.put("coverImage", a.getCoverImage());
        m.put("publishedAt", a.getPublishedAt());
        m.put("viewCount", a.getViewCount());
        m.put("likeCount", a.getLikeCount());
        m.put("commentCount", a.getCommentCount());
        m.put("wordCount", a.getWordCount());
        m.put("isFeatured", a.getIsFeatured());
        m.put("isTop", a.getIsTop());
        m.put("categoryName", a.getCategoryId() != null ? catNameMap.get(a.getCategoryId()) : null);

        List<ArticleTag> tags = articleTagsMap.getOrDefault(a.getId(), Collections.emptyList());
        List<Map<String, Object>> tagList = tags.stream().map(t -> {
            Map<String, Object> tm = new LinkedHashMap<>();
            tm.put("id", t.getId());
            tm.put("name", t.getName());
            tm.put("slug", t.getSlug());
            return tm;
        }).collect(Collectors.toList());
        m.put("tags", tagList);
        return m;
    }
}
