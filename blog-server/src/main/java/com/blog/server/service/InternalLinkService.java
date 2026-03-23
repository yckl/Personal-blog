package com.blog.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.server.entity.*;
import com.blog.server.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Internal Link Suggestion Service.
 *
 * Suggests related articles for internal linking based on:
 * 1. Shared tags (highest weight)
 * 2. Same category
 * 3. Keyword matching in title/excerpt
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InternalLinkService {

    private final ArticleMapper articleMapper;
    private final ArticleTagRelMapper tagRelMapper;

    /**
     * Get internal link suggestions for a given article.
     * Returns articles ranked by relevance (shared tags → same category → keyword).
     */
    public List<Map<String, Object>> suggest(Long articleId, int limit) {
        Article source = articleMapper.selectById(articleId);
        if (source == null) return Collections.emptyList();

        // Get source article's tags
        List<ArticleTagRel> sourceTagRels = tagRelMapper.selectList(
                new LambdaQueryWrapper<ArticleTagRel>().eq(ArticleTagRel::getArticleId, articleId));
        Set<Long> sourceTagIds = sourceTagRels.stream()
                .map(ArticleTagRel::getTagId).collect(Collectors.toSet());

        // Get all published articles except self
        List<Article> candidates = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getStatus, "PUBLISHED")
                        .ne(Article::getId, articleId)
                        .orderByDesc(Article::getViewCount));

        // Score each candidate
        List<ScoredArticle> scored = new ArrayList<>();
        for (Article candidate : candidates) {
            double score = 0;

            // Tag overlap scoring
            if (!sourceTagIds.isEmpty()) {
                List<ArticleTagRel> candidateRels = tagRelMapper.selectList(
                        new LambdaQueryWrapper<ArticleTagRel>().eq(ArticleTagRel::getArticleId, candidate.getId()));
                Set<Long> candidateTagIds = candidateRels.stream()
                        .map(ArticleTagRel::getTagId).collect(Collectors.toSet());

                long sharedTags = candidateTagIds.stream().filter(sourceTagIds::contains).count();
                score += sharedTags * 3.0; // 3 points per shared tag
            }

            // Same category scoring
            if (source.getCategoryId() != null && source.getCategoryId().equals(candidate.getCategoryId())) {
                score += 2.0;
            }

            // Keyword matching in title
            if (StringUtils.hasText(source.getTitle()) && StringUtils.hasText(candidate.getTitle())) {
                Set<String> sourceWords = extractKeywords(source.getTitle());
                Set<String> candidateWords = extractKeywords(candidate.getTitle());
                long sharedWords = candidateWords.stream().filter(sourceWords::contains).count();
                score += sharedWords * 0.5;
            }

            if (score > 0) {
                scored.add(new ScoredArticle(candidate, score));
            }
        }

        // Sort by score desc, take top N
        scored.sort((a, b) -> Double.compare(b.score, a.score));

        return scored.stream()
                .limit(limit)
                .map(sa -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", sa.article.getId());
                    m.put("title", sa.article.getTitle());
                    m.put("slug", sa.article.getSlug());
                    m.put("excerpt", sa.article.getExcerpt());
                    m.put("score", Math.round(sa.score * 10) / 10.0);
                    m.put("url", "/article/" + sa.article.getSlug());
                    m.put("insertHtml", String.format(
                            "<a href=\"/article/%s\">%s</a>",
                            sa.article.getSlug(), sa.article.getTitle()));
                    return m;
                })
                .collect(Collectors.toList());
    }

    /**
     * Extract keywords for title matching.
     * Filters short/common words.
     */
    private Set<String> extractKeywords(String text) {
        if (text == null) return Collections.emptySet();
        Set<String> stopWords = Set.of("the", "a", "an", "is", "are", "was", "were", "in", "on",
                "at", "to", "for", "of", "and", "or", "but", "not", "with", "by", "from",
                "as", "it", "its", "this", "that", "be", "has", "have", "had", "do", "does",
                "how", "what", "when", "where", "which", "who", "whom", "why",
                "的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一", "个",
                "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好",
                "与", "及", "把", "被", "让", "给", "为", "如何", "怎么", "什么");

        return Arrays.stream(text.toLowerCase().split("[\\s,;.!?，。；！？、\\-—()（）\\[\\]\"']+"))
                .filter(w -> w.length() > 1)
                .filter(w -> !stopWords.contains(w))
                .collect(Collectors.toSet());
    }

    private record ScoredArticle(Article article, double score) {}
}
