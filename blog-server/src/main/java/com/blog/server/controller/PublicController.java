package com.blog.server.controller;

import com.blog.server.common.PageResult;
import com.blog.server.common.Result;
import com.blog.server.dto.response.ArticleVO;
import com.blog.server.entity.*;
import com.blog.server.service.ArticleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.server.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Public API endpoints (no authentication required).
 * All routes under /api/public/** are permitted in SecurityConfig.
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final ArticleService articleService;
    private final ArticleMapper articleMapper;
    private final ArticleCategoryMapper categoryMapper;
    private final ArticleTagMapper tagMapper;
    private final ArticleTagRelMapper tagRelMapper;
    private final SiteConfigMapper siteConfigMapper;
    private final MenuConfigMapper menuConfigMapper;

    // ---- Articles (public) ----

    @GetMapping("/articles")
    public Result<PageResult<ArticleVO>> listArticles(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String keyword) {
        return Result.ok(articleService.listPublishedArticles(page, size, categoryId, tagId, keyword));
    }

    @GetMapping("/articles/{slug}")
    public Result<ArticleVO> getArticleBySlug(@PathVariable String slug) {
        ArticleVO vo = articleService.getArticleBySlug(slug);
        // Increment view count
        articleService.incrementViewCount(vo.getId());
        return Result.ok(vo);
    }

    // ---- Categories ----

    @GetMapping("/categories")
    public Result<List<ArticleCategory>> listCategories() {
        return Result.ok(categoryMapper.selectList(
                new LambdaQueryWrapper<ArticleCategory>().orderByAsc(ArticleCategory::getSortOrder)));
    }

    // ---- Tags ----

    @GetMapping("/tags")
    public Result<List<ArticleTag>> listTags() {
        return Result.ok(tagMapper.selectList(new LambdaQueryWrapper<ArticleTag>()));
    }

    // ---- Site Config ----

    @GetMapping("/config/site")
    public Result<Map<String, String>> getSiteConfig() {
        List<SiteConfig> configs = siteConfigMapper.selectList(new LambdaQueryWrapper<>());
        Map<String, String> configMap = configs.stream()
                .collect(Collectors.toMap(SiteConfig::getConfigKey, 
                        c -> c.getConfigValue() != null ? c.getConfigValue() : ""));
        return Result.ok(configMap);
    }

    // ---- Menus ----

    @GetMapping("/config/menus")
    public Result<List<MenuConfig>> getMenus() {
        return Result.ok(menuConfigMapper.selectList(
                new LambdaQueryWrapper<MenuConfig>().orderByAsc(MenuConfig::getSortOrder)));
    }

    // ---- Article Recommendation ----

    @GetMapping("/articles/{id}/related")
    public Result<List<Map<String, Object>>> getRelatedArticles(@PathVariable Long id) {
        Article article = articleMapper.selectById(id);
        if (article == null) return Result.ok(Collections.emptyList());

        // Find articles sharing the same tags
        List<ArticleTagRel> tagRels = tagRelMapper.selectList(
                new LambdaQueryWrapper<ArticleTagRel>().eq(ArticleTagRel::getArticleId, id));
        Set<Long> tagIds = tagRels.stream().map(ArticleTagRel::getTagId).collect(Collectors.toSet());

        Set<Long> relatedIds = new LinkedHashSet<>();
        if (!tagIds.isEmpty()) {
            List<ArticleTagRel> sharedRels = tagRelMapper.selectList(
                    new LambdaQueryWrapper<ArticleTagRel>().in(ArticleTagRel::getTagId, tagIds).ne(ArticleTagRel::getArticleId, id));
            sharedRels.forEach(r -> relatedIds.add(r.getArticleId()));
        }
        // Also add articles in the same category
        if (article.getCategoryId() != null) {
            List<Article> sameCat = articleMapper.selectList(
                    new LambdaQueryWrapper<Article>()
                            .eq(Article::getCategoryId, article.getCategoryId())
                            .eq(Article::getStatus, "PUBLISHED")
                            .ne(Article::getId, id).last("LIMIT 10"));
            sameCat.forEach(a -> relatedIds.add(a.getId()));
        }

        if (relatedIds.isEmpty()) return Result.ok(Collections.emptyList());

        List<Article> related = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .in(Article::getId, relatedIds)
                        .eq(Article::getStatus, "PUBLISHED")
                        .last("LIMIT 6"));

        List<Map<String, Object>> result = related.stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("title", a.getTitle());
            m.put("slug", a.getSlug());
            m.put("excerpt", a.getExcerpt());
            m.put("coverImage", a.getCoverImage());
            m.put("publishedAt", a.getPublishedAt());
            m.put("viewCount", a.getViewCount());
            return m;
        }).collect(Collectors.toList());
        return Result.ok(result);
    }

    // ---- Prev / Next Navigation ----

    @GetMapping("/articles/{id}/nav")
    public Result<Map<String, Object>> getArticleNav(@PathVariable Long id) {
        Map<String, Object> nav = new LinkedHashMap<>();

        // Previous article (published before this one, or lower ID)
        Article current = articleMapper.selectById(id);
        if (current != null && current.getPublishedAt() != null) {
            Article prev = articleMapper.selectOne(
                    new LambdaQueryWrapper<Article>()
                            .eq(Article::getStatus, "PUBLISHED")
                            .lt(Article::getPublishedAt, current.getPublishedAt())
                            .orderByDesc(Article::getPublishedAt)
                            .last("LIMIT 1"));
            if (prev != null) {
                Map<String, Object> p = new LinkedHashMap<>();
                p.put("id", prev.getId());
                p.put("title", prev.getTitle());
                p.put("slug", prev.getSlug());
                nav.put("prev", p);
            }

            Article next = articleMapper.selectOne(
                    new LambdaQueryWrapper<Article>()
                            .eq(Article::getStatus, "PUBLISHED")
                            .gt(Article::getPublishedAt, current.getPublishedAt())
                            .orderByAsc(Article::getPublishedAt)
                            .last("LIMIT 1"));
            if (next != null) {
                Map<String, Object> n = new LinkedHashMap<>();
                n.put("id", next.getId());
                n.put("title", next.getTitle());
                n.put("slug", next.getSlug());
                nav.put("next", n);
            }
        }
        return Result.ok(nav);
    }
}
