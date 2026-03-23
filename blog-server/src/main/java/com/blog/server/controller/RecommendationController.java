package com.blog.server.controller;

import com.blog.server.common.Result;
import com.blog.server.entity.RecommendEvent;
import com.blog.server.mapper.RecommendEventMapper;
import com.blog.server.service.RecommendationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Recommendation API — public endpoints for article recommendations
 * and event tracking (exposure/click).
 */
@RestController
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final RecommendEventMapper recommendEventMapper;

    /**
     * Get recommendations for a specific article.
     * @param articleId source article ID
     * @param scene context: DETAIL, HOME, SEARCH_EMPTY, NOT_FOUND
     * @param size number of recommendations
     */
    @GetMapping("/api/public/recommend/articles/{articleId}")
    public Result<List<Map<String, Object>>> getRecommendations(
            @PathVariable Long articleId,
            @RequestParam(defaultValue = "DETAIL") String scene,
            @RequestParam(defaultValue = "6") Integer size) {
        return Result.ok(recommendationService.recommend(articleId, scene, size));
    }

    /**
     * Homepage recommendations — mix of boosted, fresh, and popular.
     */
    @GetMapping("/api/public/recommend/home")
    public Result<List<Map<String, Object>>> getHomeRecommendations(
            @RequestParam(defaultValue = "8") Integer size) {
        return Result.ok(recommendationService.recommendHome(size));
    }

    /**
     * Track recommendation exposure (article was shown to user).
     */
    @PostMapping("/api/public/recommend/exposure")
    public Result<Void> trackExposure(@RequestBody Map<String, Object> body,
                                       HttpServletRequest request) {
        try {
            RecommendEvent event = new RecommendEvent();
            event.setArticleId(toLong(body.get("articleId")));
            event.setRecommendedArticleId(toLong(body.get("recommendedArticleId")));
            event.setScene((String) body.getOrDefault("scene", "DETAIL"));
            event.setEventType("EXPOSURE");
            event.setUserIp(request.getRemoteAddr());
            recommendEventMapper.insert(event);
        } catch (Exception ignored) {}
        return Result.ok(null);
    }

    /**
     * Track recommendation click (user clicked a recommended article).
     */
    @PostMapping("/api/public/recommend/click")
    public Result<Void> trackClick(@RequestBody Map<String, Object> body,
                                    HttpServletRequest request) {
        try {
            RecommendEvent event = new RecommendEvent();
            event.setArticleId(toLong(body.get("articleId")));
            event.setRecommendedArticleId(toLong(body.get("recommendedArticleId")));
            event.setScene((String) body.getOrDefault("scene", "DETAIL"));
            event.setEventType("CLICK");
            event.setUserIp(request.getRemoteAddr());
            recommendEventMapper.insert(event);
        } catch (Exception ignored) {}
        return Result.ok(null);
    }

    /**
     * Admin: view recommendation results and reasons for a specific article.
     */
    @GetMapping("/api/admin/recommend/article/{articleId}")
    public Result<List<Map<String, Object>>> adminViewRecommendations(
            @PathVariable Long articleId,
            @RequestParam(defaultValue = "DETAIL") String scene,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.ok(recommendationService.recommend(articleId, scene, size));
    }

    private Long toLong(Object val) {
        if (val == null) return 0L;
        if (val instanceof Number) return ((Number) val).longValue();
        return Long.parseLong(val.toString());
    }
}
