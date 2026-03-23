package com.blog.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.server.common.Result;
import com.blog.server.entity.*;
import com.blog.server.mapper.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Public search API — full-text search across articles, suggest, and logging.
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class SearchController {

    private final ArticleMapper articleMapper;
    private final ArticleContentMapper articleContentMapper;
    private final ArticleCategoryMapper categoryMapper;
    private final ArticleTagMapper tagMapper;
    private final ArticleTagRelMapper tagRelMapper;
    private final SearchLogMapper searchLogMapper;

    /**
     * Full search: title + excerpt + content body.
     * Supports optional category and tag filters.
     */
    @GetMapping("/search")
    public Result<Map<String, Object>> search(
            @RequestParam String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            HttpServletRequest request) {

        if (!StringUtils.hasText(keyword) || keyword.trim().length() < 1) {
            return Result.ok(Map.of("results", Collections.emptyList(), "total", 0));
        }

        String kw = keyword.trim();

        // --- 1. Title + excerpt match (high relevance) ---
        LambdaQueryWrapper<Article> titleQuery = new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, "PUBLISHED")
                .and(w -> w.like(Article::getTitle, kw).or().like(Article::getExcerpt, kw));
        if (categoryId != null) titleQuery.eq(Article::getCategoryId, categoryId);
        List<Article> titleMatches = articleMapper.selectList(titleQuery);

        // --- 2. Content body match (lower relevance) ---
        LambdaQueryWrapper<ArticleContent> contentQuery = new LambdaQueryWrapper<ArticleContent>()
                .like(ArticleContent::getContentMd, kw);
        List<ArticleContent> contentMatches = articleContentMapper.selectList(contentQuery);
        Set<Long> contentArticleIds = contentMatches.stream()
                .map(ArticleContent::getArticleId).collect(Collectors.toSet());

        // Merge: title matches first, then content-only matches
        Set<Long> titleIds = titleMatches.stream().map(Article::getId).collect(Collectors.toSet());
        Set<Long> contentOnlyIds = new LinkedHashSet<>(contentArticleIds);
        contentOnlyIds.removeAll(titleIds);

        List<Article> allMatches = new ArrayList<>(titleMatches);
        if (!contentOnlyIds.isEmpty()) {
            List<Article> contentArticles = articleMapper.selectList(
                    new LambdaQueryWrapper<Article>()
                            .in(Article::getId, contentOnlyIds)
                            .eq(Article::getStatus, "PUBLISHED"));
            if (categoryId != null) {
                contentArticles.removeIf(a -> !categoryId.equals(a.getCategoryId()));
            }
            allMatches.addAll(contentArticles);
        }

        // --- 3. Tag filter ---
        if (tagId != null) {
            List<ArticleTagRel> rels = tagRelMapper.selectList(
                    new LambdaQueryWrapper<ArticleTagRel>().eq(ArticleTagRel::getTagId, tagId));
            Set<Long> tagArticleIds = rels.stream().map(ArticleTagRel::getArticleId).collect(Collectors.toSet());
            allMatches.removeIf(a -> !tagArticleIds.contains(a.getId()));
        }

        int total = allMatches.size();

        // Paginate
        int start = (page - 1) * size;
        int end = Math.min(start + size, allMatches.size());
        List<Article> pageResults = start < allMatches.size() ? allMatches.subList(start, end) : Collections.emptyList();

        // Preload categories
        List<ArticleCategory> cats = categoryMapper.selectList(new LambdaQueryWrapper<>());
        Map<Long, String> catMap = cats.stream()
                .collect(Collectors.toMap(ArticleCategory::getId, ArticleCategory::getName, (a, b) -> a));

        // Preload tags
        List<ArticleTag> allTags = tagMapper.selectList(new LambdaQueryWrapper<>());
        Map<Long, ArticleTag> tagMap = allTags.stream()
                .collect(Collectors.toMap(ArticleTag::getId, t -> t, (a, b) -> a));
        List<ArticleTagRel> allRels = tagRelMapper.selectList(new LambdaQueryWrapper<>());
        Map<Long, List<Map<String, Object>>> articleTagsMap = new HashMap<>();
        for (ArticleTagRel rel : allRels) {
            ArticleTag tag = tagMap.get(rel.getTagId());
            if (tag != null) {
                Map<String, Object> tm = new LinkedHashMap<>();
                tm.put("id", tag.getId());
                tm.put("name", tag.getName());
                tm.put("slug", tag.getSlug());
                articleTagsMap.computeIfAbsent(rel.getArticleId(), k -> new ArrayList<>()).add(tm);
            }
        }

        // Build results with highlight
        List<Map<String, Object>> results = pageResults.stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("title", a.getTitle());
            m.put("slug", a.getSlug());
            m.put("excerpt", a.getExcerpt());
            m.put("coverImage", a.getCoverImage());
            m.put("publishedAt", a.getPublishedAt());
            m.put("viewCount", a.getViewCount());
            m.put("wordCount", a.getWordCount());
            m.put("categoryName", a.getCategoryId() != null ? catMap.get(a.getCategoryId()) : null);
            m.put("tags", articleTagsMap.getOrDefault(a.getId(), Collections.emptyList()));
            m.put("matchType", titleIds.contains(a.getId()) ? "title" : "content");
            return m;
        }).collect(Collectors.toList());

        // --- Log search ---
        try {
            SearchLog log = new SearchLog();
            log.setKeyword(kw);
            log.setResultCount(total);
            log.setUserIp(request.getRemoteAddr());
            searchLogMapper.insert(log);
        } catch (Exception ignored) {}

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("results", results);
        response.put("total", total);
        response.put("page", page);
        response.put("size", size);
        return Result.ok(response);
    }

    /**
     * Search suggestions — returns matching article titles for autocomplete.
     */
    @GetMapping("/search/suggest")
    public Result<List<Map<String, Object>>> suggest(@RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false, name = "q") String q) {
        String actualKeyword = StringUtils.hasText(keyword) ? keyword : q;
        if (!StringUtils.hasText(actualKeyword) || actualKeyword.trim().length() < 1) {
            return Result.ok(Collections.emptyList());
        }

        List<Article> matches = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getStatus, "PUBLISHED")
                        .like(Article::getTitle, actualKeyword.trim())
                        .orderByDesc(Article::getViewCount)
                        .last("LIMIT 8"));

        List<Map<String, Object>> suggestions = matches.stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("title", a.getTitle());
            m.put("slug", a.getSlug());
            return m;
        }).collect(Collectors.toList());

        return Result.ok(suggestions);
    }

    /**
     * Popular search keywords (from search log).
     */
    @GetMapping("/search/popular")
    public Result<List<String>> popularKeywords() {
        List<SearchLog> logs = searchLogMapper.selectList(
                new LambdaQueryWrapper<SearchLog>()
                        .select(SearchLog::getKeyword)
                        .groupBy(SearchLog::getKeyword)
                        .orderByDesc(SearchLog::getId)
                        .last("LIMIT 10"));
        List<String> keywords = logs.stream()
                .map(SearchLog::getKeyword)
                .distinct()
                .collect(Collectors.toList());
        return Result.ok(keywords);
    }
}
