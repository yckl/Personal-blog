package com.blog.server.controller;

import com.blog.server.common.Result;
import com.blog.server.dto.response.DashboardSummary;
import com.blog.server.dto.response.DashboardTrendPoint;
import com.blog.server.dto.response.HotArticle;
import com.blog.server.dto.response.RecentComment;
import com.blog.server.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Summary statistics: view counts, subscribers, articles, comments, pending items.
     */
    @GetMapping("/summary")
    public Result<DashboardSummary> getSummary() {
        return Result.ok(dashboardService.getSummary());
    }

    /**
     * View trend data for the chart.
     * @param days number of days (default 7, max 90)
     */
    @GetMapping("/trend")
    public Result<List<DashboardTrendPoint>> getTrend(
            @RequestParam(defaultValue = "7") int days) {
        return Result.ok(dashboardService.getTrend(days));
    }

    /**
     * Top articles by view count.
     * @param limit number of articles (default 10, max 20)
     */
    @GetMapping("/hot-articles")
    public Result<List<HotArticle>> getHotArticles(
            @RequestParam(defaultValue = "10") int limit) {
        return Result.ok(dashboardService.getHotArticles(limit));
    }

    /**
     * Most recent comments with article titles.
     * @param limit number of comments (default 10, max 20)
     */
    @GetMapping("/recent-comments")
    public Result<List<RecentComment>> getRecentComments(
            @RequestParam(defaultValue = "10") int limit) {
        return Result.ok(dashboardService.getRecentComments(limit));
    }
}
