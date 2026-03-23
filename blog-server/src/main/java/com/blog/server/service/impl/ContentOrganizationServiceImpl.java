package com.blog.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.server.entity.*;
import com.blog.server.mapper.*;
import com.blog.server.exception.BusinessException;
import com.blog.server.service.ContentOrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContentOrganizationServiceImpl implements ContentOrganizationService {

    private final ArticleCategoryMapper categoryMapper;
    private final ArticleTagMapper tagMapper;
    private final ArticleTagRelMapper tagRelMapper;
    private final ArticleMapper articleMapper;
    private final ArticleSeriesMapper seriesMapper;
    private final ArticleSeriesRelMapper seriesRelMapper;

    // ============ Categories ============

    @Override
    public List<Map<String, Object>> getCategoryTree() {
        List<ArticleCategory> all = categoryMapper.selectList(
                new LambdaQueryWrapper<ArticleCategory>().orderByAsc(ArticleCategory::getSortOrder));

        // Count articles per category
        List<Article> publishedArticles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>().eq(Article::getStatus, "PUBLISHED")
                        .select(Article::getId, Article::getCategoryId));
        Map<Long, Long> countMap = publishedArticles.stream()
                .filter(a -> a.getCategoryId() != null)
                .collect(Collectors.groupingBy(Article::getCategoryId, Collectors.counting()));

        // Build tree
        Map<Long, List<Map<String, Object>>> childrenMap = new LinkedHashMap<>();
        List<Map<String, Object>> roots = new ArrayList<>();

        for (ArticleCategory cat : all) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", cat.getId());
            node.put("name", cat.getName());
            node.put("slug", cat.getSlug());
            node.put("description", cat.getDescription());
            node.put("parentId", cat.getParentId());
            node.put("articleCount", countMap.getOrDefault(cat.getId(), 0L));
            node.put("children", new ArrayList<>());

            if (cat.getParentId() == null || cat.getParentId() == 0) {
                roots.add(node);
            } else {
                childrenMap.computeIfAbsent(cat.getParentId(), k -> new ArrayList<>()).add(node);
            }
        }

        // Attach children
        for (Map<String, Object> root : roots) {
            attachChildren(root, childrenMap);
        }
        return roots;
    }

    @SuppressWarnings("unchecked")
    private void attachChildren(Map<String, Object> node, Map<Long, List<Map<String, Object>>> childrenMap) {
        Long id = ((Number) node.get("id")).longValue();
        List<Map<String, Object>> children = childrenMap.get(id);
        if (children != null) {
            ((List<Map<String, Object>>) node.get("children")).addAll(children);
            for (Map<String, Object> child : children) {
                attachChildren(child, childrenMap);
            }
        }
    }

    @Override
    public List<Map<String, Object>> getArticlesByCategory(String slug) {
        ArticleCategory cat = categoryMapper.selectOne(
                new LambdaQueryWrapper<ArticleCategory>().eq(ArticleCategory::getSlug, slug));
        if (cat == null) throw new BusinessException(404, "Category not found");

        // Get all child category IDs recursively
        List<Long> categoryIds = new ArrayList<>();
        categoryIds.add(cat.getId());
        collectChildIds(cat.getId(), categoryIds);

        List<Article> articles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getStatus, "PUBLISHED")
                        .in(Article::getCategoryId, categoryIds)
                        .orderByDesc(Article::getPublishedAt));

        return articles.stream().map(this::toArticleSummary).collect(Collectors.toList());
    }

    private void collectChildIds(Long parentId, List<Long> result) {
        List<ArticleCategory> children = categoryMapper.selectList(
                new LambdaQueryWrapper<ArticleCategory>().eq(ArticleCategory::getParentId, parentId));
        for (ArticleCategory child : children) {
            result.add(child.getId());
            collectChildIds(child.getId(), result);
        }
    }

    // ============ Tags ============

    @Override
    public List<Map<String, Object>> getTagsWithCount() {
        List<ArticleTag> allTags = tagMapper.selectList(new LambdaQueryWrapper<>());
        List<ArticleTagRel> allRels = tagRelMapper.selectList(new LambdaQueryWrapper<>());

        // Only count published articles
        Set<Long> publishedIds = articleMapper.selectList(
                new LambdaQueryWrapper<Article>().eq(Article::getStatus, "PUBLISHED").select(Article::getId))
                .stream().map(Article::getId).collect(Collectors.toSet());

        Map<Long, Long> tagCount = allRels.stream()
                .filter(r -> publishedIds.contains(r.getArticleId()))
                .collect(Collectors.groupingBy(ArticleTagRel::getTagId, Collectors.counting()));

        return allTags.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("name", t.getName());
            m.put("slug", t.getSlug());
            m.put("articleCount", tagCount.getOrDefault(t.getId(), 0L));
            return m;
        }).sorted((a, b) -> Long.compare((Long) b.get("articleCount"), (Long) a.get("articleCount")))
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getArticlesByTag(String slug) {
        ArticleTag tag = tagMapper.selectOne(
                new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getSlug, slug));
        if (tag == null) throw new BusinessException(404, "Tag not found");

        List<Long> articleIds = tagRelMapper.selectList(
                new LambdaQueryWrapper<ArticleTagRel>().eq(ArticleTagRel::getTagId, tag.getId()))
                .stream().map(ArticleTagRel::getArticleId).collect(Collectors.toList());

        if (articleIds.isEmpty()) return Collections.emptyList();

        return articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getStatus, "PUBLISHED")
                        .in(Article::getId, articleIds)
                        .orderByDesc(Article::getPublishedAt))
                .stream().map(this::toArticleSummary).collect(Collectors.toList());
    }

    // ============ Series ============

    @Override
    public List<Map<String, Object>> getSeriesWithCount() {
        List<ArticleSeries> allSeries = seriesMapper.selectList(
                new LambdaQueryWrapper<ArticleSeries>().orderByAsc(ArticleSeries::getSortOrder));
        List<ArticleSeriesRel> allRels = seriesRelMapper.selectList(new LambdaQueryWrapper<>());

        Map<Long, Long> countMap = allRels.stream()
                .collect(Collectors.groupingBy(ArticleSeriesRel::getSeriesId, Collectors.counting()));

        return allSeries.stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("name", s.getName());
            m.put("slug", s.getSlug());
            m.put("description", s.getDescription());
            m.put("coverImage", s.getCoverImage());
            m.put("articleCount", countMap.getOrDefault(s.getId(), 0L));
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getSeriesDetail(String slug) {
        ArticleSeries series = seriesMapper.selectOne(
                new LambdaQueryWrapper<ArticleSeries>().eq(ArticleSeries::getSlug, slug));
        if (series == null) throw new BusinessException(404, "Series not found");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", series.getId());
        result.put("name", series.getName());
        result.put("slug", series.getSlug());
        result.put("description", series.getDescription());
        result.put("coverImage", series.getCoverImage());

        // Get articles in order
        List<ArticleSeriesRel> rels = seriesRelMapper.selectList(
                new LambdaQueryWrapper<ArticleSeriesRel>()
                        .eq(ArticleSeriesRel::getSeriesId, series.getId())
                        .orderByAsc(ArticleSeriesRel::getSortOrder));

        List<Map<String, Object>> articles = new ArrayList<>();
        for (ArticleSeriesRel rel : rels) {
            Article article = articleMapper.selectById(rel.getArticleId());
            if (article != null) {
                Map<String, Object> a = toArticleSummary(article);
                a.put("sortOrder", rel.getSortOrder());
                articles.add(a);
            }
        }
        result.put("articles", articles);
        result.put("articleCount", articles.size());
        return result;
    }

    // ============ Archive ============

    @Override
    public List<Map<String, Object>> getArchive() {
        List<Article> published = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getStatus, "PUBLISHED")
                        .isNotNull(Article::getPublishedAt)
                        .orderByDesc(Article::getPublishedAt)
                        .select(Article::getId, Article::getPublishedAt));

        // Group by year-month
        Map<String, Long> grouped = new LinkedHashMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        for (Article a : published) {
            String key = a.getPublishedAt().format(fmt);
            grouped.merge(key, 1L, (x, y) -> x + y);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : grouped.entrySet()) {
            String[] parts = entry.getKey().split("-");
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("year", Integer.parseInt(parts[0]));
            m.put("month", Integer.parseInt(parts[1]));
            m.put("yearMonth", entry.getKey());
            m.put("count", entry.getValue());
            result.add(m);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getArchiveArticles(int year, Integer month) {
        List<Article> all = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getStatus, "PUBLISHED")
                        .isNotNull(Article::getPublishedAt)
                        .orderByDesc(Article::getPublishedAt));

        return all.stream()
                .filter(a -> a.getPublishedAt().getYear() == year)
                .filter(a -> month == null || a.getPublishedAt().getMonthValue() == month)
                .map(this::toArticleSummary)
                .collect(Collectors.toList());
    }

    // ---- Helpers ----

    private Map<String, Object> toArticleSummary(Article a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("title", a.getTitle());
        m.put("slug", a.getSlug());
        m.put("excerpt", a.getExcerpt());
        m.put("coverImage", a.getCoverImage());
        m.put("status", a.getStatus());
        m.put("publishedAt", a.getPublishedAt());
        m.put("viewCount", a.getViewCount());
        m.put("likeCount", a.getLikeCount());
        m.put("commentCount", a.getCommentCount());
        return m;
    }
}
