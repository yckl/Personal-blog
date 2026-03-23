package com.blog.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.server.entity.*;
import com.blog.server.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Advanced Recommendation Service with multi-signal scoring.
 * 
 * Pipeline: Recall → Score → Rerank → Output
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final ArticleMapper articleMapper;
    private final ArticleTagRelMapper tagRelMapper;
    private final ArticleSimilarityMapper similarityMapper;
    private final ArticleManualBoostMapper manualBoostMapper;
    private final ArticleSeriesRelMapper seriesRelMapper;

    // --- Scoring weights ---
    private static final double W_TAG_OVERLAP = 3.0;
    private static final double W_CATEGORY_MATCH = 2.0;
    private static final double W_SERIES = 5.0;
    private static final double W_POPULARITY = 1.5;
    private static final double W_FRESHNESS = 1.0;
    private static final double W_MANUAL_BOOST = 4.0;
    private static final double W_SIMILARITY = 2.5;

    /**
     * Main recommendation entry point.
     */
    public List<Map<String, Object>> recommend(Long articleId, String scene, int size) {
        Article sourceArticle = articleMapper.selectById(articleId);
        if (sourceArticle == null) return fallbackRecommend(size);

        // 1. RECALL: gather candidates from multiple sources
        Set<Long> candidateIds = new LinkedHashSet<>();
        recall(sourceArticle, candidateIds);
        candidateIds.remove(articleId); // exclude self

        if (candidateIds.isEmpty()) return fallbackRecommend(size);

        // Load candidate articles
        List<Article> candidates = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .in(Article::getId, candidateIds)
                        .eq(Article::getStatus, "PUBLISHED"));

        if (candidates.isEmpty()) return fallbackRecommend(size);

        // 2. SCORE: compute recommendation score for each candidate
        Map<Long, Double> scores = new LinkedHashMap<>();
        Map<Long, List<String>> reasons = new LinkedHashMap<>();
        for (Article candidate : candidates) {
            double score = 0;
            List<String> r = new ArrayList<>();

            // Tag overlap
            double tagScore = computeTagOverlap(sourceArticle.getId(), candidate.getId());
            if (tagScore > 0) { score += W_TAG_OVERLAP * tagScore; r.add("Tags"); }

            // Category match
            if (sourceArticle.getCategoryId() != null &&
                    sourceArticle.getCategoryId().equals(candidate.getCategoryId())) {
                score += W_CATEGORY_MATCH;
                r.add("Category");
            }

            // Series relation
            double seriesScore = computeSeriesScore(sourceArticle.getId(), candidate.getId());
            if (seriesScore > 0) { score += W_SERIES * seriesScore; r.add("Series"); }

            // Popularity (normalized 0-1)
            double popScore = computePopularity(candidate);
            score += W_POPULARITY * popScore;

            // Freshness (within 30 days gets bonus)
            double freshScore = computeFreshness(candidate);
            if (freshScore > 0) { score += W_FRESHNESS * freshScore; r.add("New"); }

            // Precomputed similarity
            double simScore = getPrecomputedSimilarity(sourceArticle.getId(), candidate.getId());
            if (simScore > 0) { score += W_SIMILARITY * simScore; r.add("Similar"); }

            // Manual boost
            double boostScore = getManualBoost(candidate.getId(), scene);
            if (boostScore > 0) { score += W_MANUAL_BOOST * boostScore; r.add("Editorial"); }

            scores.put(candidate.getId(), score);
            reasons.put(candidate.getId(), r);
        }

        // 3. SORT by score descending
        candidates.sort((a, b) -> Double.compare(scores.getOrDefault(b.getId(), 0.0),
                scores.getOrDefault(a.getId(), 0.0)));

        // 4. RERANK: diversity and freshness injection
        List<Article> reranked = rerank(candidates, scene, size);

        // 5. Build output
        return reranked.stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("title", a.getTitle());
            m.put("slug", a.getSlug());
            m.put("excerpt", a.getExcerpt());
            m.put("coverImage", a.getCoverImage());
            m.put("publishedAt", a.getPublishedAt());
            m.put("viewCount", a.getViewCount());
            m.put("score", Math.round(scores.getOrDefault(a.getId(), 0.0) * 100.0) / 100.0);
            m.put("reasons", reasons.getOrDefault(a.getId(), List.of()));
            return m;
        }).collect(Collectors.toList());
    }

    /**
     * Homepage recommendations — mix of popular, fresh, and editorially boosted.
     */
    public List<Map<String, Object>> recommendHome(int size) {
        // Manual boosts for HOME scene
        List<ArticleManualBoost> boosts = manualBoostMapper.selectList(
                new LambdaQueryWrapper<ArticleManualBoost>()
                        .in(ArticleManualBoost::getTargetScene, "ALL", "HOME")
                        .le(ArticleManualBoost::getStartAt, LocalDateTime.now())
                        .ge(ArticleManualBoost::getEndAt, LocalDateTime.now()));
        Set<Long> boostedIds = boosts.stream().map(ArticleManualBoost::getArticleId).collect(Collectors.toSet());

        // Popular articles (last 30 days)
        List<Article> popular = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getStatus, "PUBLISHED")
                        .orderByDesc(Article::getViewCount)
                        .last("LIMIT 20"));

        // Fresh articles (last 7 days)
        List<Article> fresh = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getStatus, "PUBLISHED")
                        .ge(Article::getPublishedAt, LocalDateTime.now().minusDays(7))
                        .orderByDesc(Article::getPublishedAt)
                        .last("LIMIT 10"));

        // Merge: boosted first, then mix fresh and popular
        Map<Long, Article> merged = new LinkedHashMap<>();
        if (!boostedIds.isEmpty()) {
            List<Article> boosted = articleMapper.selectList(
                    new LambdaQueryWrapper<Article>()
                            .in(Article::getId, boostedIds)
                            .eq(Article::getStatus, "PUBLISHED"));
            boosted.forEach(a -> merged.put(a.getId(), a));
        }
        fresh.forEach(a -> merged.putIfAbsent(a.getId(), a));
        popular.forEach(a -> merged.putIfAbsent(a.getId(), a));

        return merged.values().stream().limit(size).map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("title", a.getTitle());
            m.put("slug", a.getSlug());
            m.put("excerpt", a.getExcerpt());
            m.put("coverImage", a.getCoverImage());
            m.put("publishedAt", a.getPublishedAt());
            m.put("viewCount", a.getViewCount());
            m.put("boosted", boostedIds.contains(a.getId()));
            return m;
        }).collect(Collectors.toList());
    }

    // ===================== RECALL =====================

    private void recall(Article source, Set<Long> candidateIds) {
        Long sourceId = source.getId();

        // R1: Tag-based recall
        List<ArticleTagRel> sourceTagRels = tagRelMapper.selectList(
                new LambdaQueryWrapper<ArticleTagRel>().eq(ArticleTagRel::getArticleId, sourceId));
        Set<Long> tagIds = sourceTagRels.stream().map(ArticleTagRel::getTagId).collect(Collectors.toSet());
        if (!tagIds.isEmpty()) {
            List<ArticleTagRel> sharedRels = tagRelMapper.selectList(
                    new LambdaQueryWrapper<ArticleTagRel>()
                            .in(ArticleTagRel::getTagId, tagIds)
                            .ne(ArticleTagRel::getArticleId, sourceId));
            sharedRels.forEach(r -> candidateIds.add(r.getArticleId()));
        }

        // R2: Category-based recall
        if (source.getCategoryId() != null) {
            List<Article> sameCat = articleMapper.selectList(
                    new LambdaQueryWrapper<Article>()
                            .eq(Article::getCategoryId, source.getCategoryId())
                            .eq(Article::getStatus, "PUBLISHED")
                            .ne(Article::getId, sourceId).last("LIMIT 20"));
            sameCat.forEach(a -> candidateIds.add(a.getId()));
        }

        // R3: Series-based recall
        List<ArticleSeriesRel> seriesRels = seriesRelMapper.selectList(
                new LambdaQueryWrapper<ArticleSeriesRel>().eq(ArticleSeriesRel::getArticleId, sourceId));
        for (ArticleSeriesRel rel : seriesRels) {
            List<ArticleSeriesRel> sameSeriesRels = seriesRelMapper.selectList(
                    new LambdaQueryWrapper<ArticleSeriesRel>()
                            .eq(ArticleSeriesRel::getSeriesId, rel.getSeriesId())
                            .ne(ArticleSeriesRel::getArticleId, sourceId));
            sameSeriesRels.forEach(r -> candidateIds.add(r.getArticleId()));
        }

        // R4: Precomputed similarity recall
        List<ArticleSimilarity> sims = similarityMapper.selectList(
                new LambdaQueryWrapper<ArticleSimilarity>()
                        .eq(ArticleSimilarity::getArticleId, sourceId)
                        .orderByDesc(ArticleSimilarity::getScore)
                        .last("LIMIT 20"));
        sims.forEach(s -> candidateIds.add(s.getSimilarArticleId()));

        // R5: Popular recall (fallback: always include some popular articles)
        List<Article> popular = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getStatus, "PUBLISHED")
                        .ne(Article::getId, sourceId)
                        .orderByDesc(Article::getViewCount)
                        .last("LIMIT 10"));
        popular.forEach(a -> candidateIds.add(a.getId()));

        // R6: Fresh article recall (new post boosting)
        List<Article> fresh = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getStatus, "PUBLISHED")
                        .ne(Article::getId, sourceId)
                        .ge(Article::getPublishedAt, LocalDateTime.now().minusDays(7))
                        .orderByDesc(Article::getPublishedAt)
                        .last("LIMIT 5"));
        fresh.forEach(a -> candidateIds.add(a.getId()));

        // R7: Manual editorial recall
        List<ArticleManualBoost> boosts = manualBoostMapper.selectList(
                new LambdaQueryWrapper<ArticleManualBoost>()
                        .le(ArticleManualBoost::getStartAt, LocalDateTime.now())
                        .ge(ArticleManualBoost::getEndAt, LocalDateTime.now()));
        boosts.forEach(b -> candidateIds.add(b.getArticleId()));
    }

    // ===================== SCORING FACTORS =====================

    private double computeTagOverlap(Long sourceId, Long candidateId) {
        List<ArticleTagRel> sourceRels = tagRelMapper.selectList(
                new LambdaQueryWrapper<ArticleTagRel>().eq(ArticleTagRel::getArticleId, sourceId));
        List<ArticleTagRel> candidateRels = tagRelMapper.selectList(
                new LambdaQueryWrapper<ArticleTagRel>().eq(ArticleTagRel::getArticleId, candidateId));

        Set<Long> sourceTags = sourceRels.stream().map(ArticleTagRel::getTagId).collect(Collectors.toSet());
        Set<Long> candidateTags = candidateRels.stream().map(ArticleTagRel::getTagId).collect(Collectors.toSet());

        if (sourceTags.isEmpty() || candidateTags.isEmpty()) return 0;

        Set<Long> overlap = new HashSet<>(sourceTags);
        overlap.retainAll(candidateTags);
        // Jaccard-style similarity
        Set<Long> union = new HashSet<>(sourceTags);
        union.addAll(candidateTags);
        return (double) overlap.size() / union.size();
    }

    private double computeSeriesScore(Long sourceId, Long candidateId) {
        List<ArticleSeriesRel> sourceRels = seriesRelMapper.selectList(
                new LambdaQueryWrapper<ArticleSeriesRel>().eq(ArticleSeriesRel::getArticleId, sourceId));
        List<ArticleSeriesRel> candRels = seriesRelMapper.selectList(
                new LambdaQueryWrapper<ArticleSeriesRel>().eq(ArticleSeriesRel::getArticleId, candidateId));

        for (ArticleSeriesRel sr : sourceRels) {
            for (ArticleSeriesRel cr : candRels) {
                if (sr.getSeriesId().equals(cr.getSeriesId())) {
                    // Same series — bonus for adjacent articles
                    int dist = Math.abs(sr.getSortOrder() - cr.getSortOrder());
                    if (dist == 1) return 1.0; // next/prev in series
                    return 0.6 / dist; // further in series
                }
            }
        }
        return 0;
    }

    private double computePopularity(Article article) {
        int viewCount = article.getViewCount() != null ? article.getViewCount() : 0;
        // Log-normalized popularity (0-1 range, caps at ~10000 views)
        return Math.min(1.0, Math.log1p(viewCount) / Math.log1p(10000));
    }

    private double computeFreshness(Article article) {
        if (article.getPublishedAt() == null) return 0;
        long daysSincePublished = ChronoUnit.DAYS.between(article.getPublishedAt(), LocalDateTime.now());
        if (daysSincePublished < 0) daysSincePublished = 0;
        if (daysSincePublished > 30) return 0;
        // Linear decay: 1.0 for today, 0.0 for 30+ days
        return 1.0 - (daysSincePublished / 30.0);
    }

    private double getPrecomputedSimilarity(Long sourceId, Long candidateId) {
        ArticleSimilarity sim = similarityMapper.selectOne(
                new LambdaQueryWrapper<ArticleSimilarity>()
                        .eq(ArticleSimilarity::getArticleId, sourceId)
                        .eq(ArticleSimilarity::getSimilarArticleId, candidateId));
        return sim != null ? sim.getScore() : 0;
    }

    private double getManualBoost(Long articleId, String scene) {
        List<ArticleManualBoost> boosts = manualBoostMapper.selectList(
                new LambdaQueryWrapper<ArticleManualBoost>()
                        .eq(ArticleManualBoost::getArticleId, articleId)
                        .in(ArticleManualBoost::getTargetScene, "ALL", scene)
                        .le(ArticleManualBoost::getStartAt, LocalDateTime.now())
                        .ge(ArticleManualBoost::getEndAt, LocalDateTime.now()));
        return boosts.stream()
                .mapToDouble(b -> b.getBoostLevel() != null ? b.getBoostLevel() : 0)
                .max().orElse(0);
    }

    // ===================== RERANKING =====================

    private List<Article> rerank(List<Article> sorted, String scene, int size) {
        List<Article> result = new ArrayList<>();
        Map<Long, Integer> categoryCount = new HashMap<>();
        boolean freshInjected = false;

        for (Article a : sorted) {
            if (result.size() >= size) break;

            // Diversity: max 3 articles per category
            Long catId = a.getCategoryId();
            if (catId != null) {
                int count = categoryCount.getOrDefault(catId, 0);
                if (count >= 3) continue;
                categoryCount.put(catId, count + 1);
            }

            result.add(a);

            // Fresh injection: insert a fresh article at position 2-3
            if (!freshInjected && result.size() == 2) {
                Article freshArticle = findFreshArticle(sorted, result);
                if (freshArticle != null) {
                    result.add(freshArticle);
                    freshInjected = true;
                }
            }
        }

        return result;
    }

    private Article findFreshArticle(List<Article> all, List<Article> alreadyAdded) {
        Set<Long> addedIds = alreadyAdded.stream().map(Article::getId).collect(Collectors.toSet());
        LocalDateTime recentCutoff = LocalDateTime.now().minusDays(7);
        return all.stream()
                .filter(a -> !addedIds.contains(a.getId()))
                .filter(a -> a.getPublishedAt() != null && a.getPublishedAt().isAfter(recentCutoff))
                .findFirst().orElse(null);
    }

    // ===================== FALLBACK =====================

    private List<Map<String, Object>> fallbackRecommend(int size) {
        List<Article> popular = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getStatus, "PUBLISHED")
                        .orderByDesc(Article::getViewCount)
                        .last("LIMIT " + size));
        return popular.stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("title", a.getTitle());
            m.put("slug", a.getSlug());
            m.put("excerpt", a.getExcerpt());
            m.put("coverImage", a.getCoverImage());
            m.put("publishedAt", a.getPublishedAt());
            m.put("viewCount", a.getViewCount());
            m.put("score", 0);
            m.put("reasons", List.of("Popular"));
            return m;
        }).collect(Collectors.toList());
    }
}
