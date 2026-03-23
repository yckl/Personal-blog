package com.blog.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.server.common.Result;
import com.blog.server.entity.*;
import com.blog.server.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Analytics API — dashboard overview, 7-day trends, traffic sources,
 * device/browser distribution, per-article stats, subscription conversion.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/analytics")
public class AnalyticsController {

    private final VisitLogMapper visitLogMapper;
    private final ArticleMapper articleMapper;
    private final SubscriberMapper subscriberMapper;

    /**
     * Full dashboard overview with 7-day trends.
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview(@RequestParam(defaultValue = "7") int days) {
        LocalDateTime since = LocalDate.now().minusDays(days).atStartOfDay();

        // Total counts
        long totalPV = visitLogMapper.selectCount(new LambdaQueryWrapper<>());
        long periodPV = visitLogMapper.selectCount(
                new LambdaQueryWrapper<VisitLog>().ge(VisitLog::getCreatedAt, since));

        // Unique visitors (by visitor_id)
        List<VisitLog> periodVisits = visitLogMapper.selectList(
                new LambdaQueryWrapper<VisitLog>()
                        .ge(VisitLog::getCreatedAt, since)
                        .select(VisitLog::getVisitorId, VisitLog::getCreatedAt,
                                VisitLog::getArticleId, VisitLog::getReferer,
                                VisitLog::getDeviceType, VisitLog::getBrowser, VisitLog::getOs));

        long periodUV = periodVisits.stream()
                .map(VisitLog::getVisitorId).filter(Objects::nonNull)
                .distinct().count();

        // 7-day PV/UV trend
        List<Map<String, Object>> dailyTrend = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

            List<VisitLog> dayVisits = periodVisits.stream()
                    .filter(v -> v.getCreatedAt() != null
                            && !v.getCreatedAt().isBefore(dayStart)
                            && !v.getCreatedAt().isAfter(dayEnd))
                    .toList();

            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", date.toString());
            day.put("pv", dayVisits.size());
            day.put("uv", dayVisits.stream().map(VisitLog::getVisitorId)
                    .filter(Objects::nonNull).distinct().count());
            dailyTrend.add(day);
        }

        // Top articles by views
        Map<Long, Long> articleViews = periodVisits.stream()
                .filter(v -> v.getArticleId() != null)
                .collect(Collectors.groupingBy(VisitLog::getArticleId, Collectors.counting()));

        List<Map<String, Object>> hotArticles = articleViews.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> {
                    Article a = articleMapper.selectById(e.getKey());
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("articleId", e.getKey());
                    m.put("title", a != null ? a.getTitle() : "Deleted");
                    m.put("slug", a != null ? a.getSlug() : "");
                    m.put("views", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());

        // Traffic sources
        Map<String, Long> refererCounts = periodVisits.stream()
                .map(v -> classifyReferer(v.getReferer()))
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

        List<Map<String, Object>> trafficSources = refererCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("source", e.getKey());
                    m.put("count", e.getValue());
                    m.put("percentage", periodPV > 0 ? Math.round(e.getValue() * 1000.0 / periodPV) / 10.0 : 0);
                    return m;
                })
                .collect(Collectors.toList());

        // Device distribution
        Map<String, Long> deviceCounts = periodVisits.stream()
                .map(v -> v.getDeviceType() != null ? v.getDeviceType() : "unknown")
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

        // Browser distribution
        Map<String, Long> browserCounts = periodVisits.stream()
                .map(v -> v.getBrowser() != null ? v.getBrowser() : "unknown")
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

        // OS distribution
        Map<String, Long> osCounts = periodVisits.stream()
                .map(v -> v.getOs() != null ? v.getOs() : "unknown")
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

        // Subscription conversion
        long totalSubscribers = subscriberMapper.selectCount(new LambdaQueryWrapper<>());
        long confirmedSubscribers = subscriberMapper.selectCount(
                new LambdaQueryWrapper<Subscriber>().eq(Subscriber::getStatus, "CONFIRMED"));
        long periodNewSubscribers = subscriberMapper.selectCount(
                new LambdaQueryWrapper<Subscriber>().ge(Subscriber::getCreatedAt, since));

        Map<String, Object> subscriptionConversion = new LinkedHashMap<>();
        subscriptionConversion.put("totalSubscribers", totalSubscribers);
        subscriptionConversion.put("confirmedSubscribers", confirmedSubscribers);
        subscriptionConversion.put("periodNewSubscribers", periodNewSubscribers);
        subscriptionConversion.put("conversionRate", periodPV > 0
                ? Math.round(periodNewSubscribers * 1000.0 / periodPV) / 10.0 : 0);

        // Subscription source breakdown
        List<Subscriber> periodSubs = subscriberMapper.selectList(
                new LambdaQueryWrapper<Subscriber>().ge(Subscriber::getCreatedAt, since));
        Map<String, Long> subSources = periodSubs.stream()
                .map(s -> s.getSource() != null ? s.getSource() : "unknown")
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));
        subscriptionConversion.put("sourceBreakdown", subSources);

        // Build response
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("period", days + " days");
        result.put("totalPV", totalPV);
        result.put("periodPV", periodPV);
        result.put("periodUV", periodUV);
        result.put("dailyTrend", dailyTrend);
        result.put("hotArticles", hotArticles);
        result.put("trafficSources", trafficSources);
        result.put("devices", deviceCounts);
        result.put("browsers", browserCounts);
        result.put("os", osCounts);
        result.put("subscriptionConversion", subscriptionConversion);

        return Result.ok(result);
    }

    /**
     * Per-article analytics.
     */
    @GetMapping("/article/{articleId}")
    public Result<Map<String, Object>> articleAnalytics(
            @PathVariable Long articleId,
            @RequestParam(defaultValue = "30") int days) {

        Article article = articleMapper.selectById(articleId);
        LocalDateTime since = LocalDate.now().minusDays(days).atStartOfDay();

        List<VisitLog> visits = visitLogMapper.selectList(
                new LambdaQueryWrapper<VisitLog>()
                        .eq(VisitLog::getArticleId, articleId)
                        .ge(VisitLog::getCreatedAt, since));

        // Daily trend
        List<Map<String, Object>> dailyTrend = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

            long dayPV = visits.stream()
                    .filter(v -> v.getCreatedAt() != null
                            && !v.getCreatedAt().isBefore(dayStart)
                            && !v.getCreatedAt().isAfter(dayEnd))
                    .count();

            if (dayPV > 0) {
                Map<String, Object> day = new LinkedHashMap<>();
                day.put("date", date.toString());
                day.put("pv", dayPV);
                dailyTrend.add(day);
            }
        }

        // Referer breakdown
        Map<String, Long> referers = visits.stream()
                .map(v -> classifyReferer(v.getReferer()))
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

        // Device breakdown
        Map<String, Long> devices = visits.stream()
                .map(v -> v.getDeviceType() != null ? v.getDeviceType() : "unknown")
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

        // Average stay
        double avgStay = visits.stream()
                .filter(v -> v.getStaySeconds() != null && v.getStaySeconds() > 0)
                .mapToInt(VisitLog::getStaySeconds)
                .average().orElse(0);

        long totalUV = visits.stream().map(VisitLog::getVisitorId)
                .filter(Objects::nonNull).distinct().count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("articleId", articleId);
        result.put("title", article != null ? article.getTitle() : "Deleted");
        result.put("period", days + " days");
        result.put("totalPV", visits.size());
        result.put("totalUV", totalUV);
        result.put("avgStaySeconds", Math.round(avgStay));
        result.put("likeCount", article != null ? article.getLikeCount() : 0);
        result.put("commentCount", article != null ? article.getCommentCount() : 0);
        result.put("dailyTrend", dailyTrend);
        result.put("trafficSources", referers);
        result.put("devices", devices);

        return Result.ok(result);
    }

    // ============ Helpers ============

    private String classifyReferer(String referer) {
        if (referer == null || referer.isEmpty()) return "Direct";
        String r = referer.toLowerCase();
        if (r.contains("google.com") || r.contains("bing.com") || r.contains("baidu.com")
                || r.contains("duckduckgo.com") || r.contains("yahoo.com")) return "Search";
        if (r.contains("twitter.com") || r.contains("x.com") || r.contains("t.co")) return "Twitter/X";
        if (r.contains("facebook.com") || r.contains("fb.com")) return "Facebook";
        if (r.contains("linkedin.com")) return "LinkedIn";
        if (r.contains("weixin.qq.com") || r.contains("weibo.com")) return "WeChat/Weibo";
        if (r.contains("github.com")) return "GitHub";
        if (r.contains("reddit.com")) return "Reddit";
        if (r.contains("news.ycombinator.com")) return "HackerNews";
        return "Other: " + extractDomain(referer);
    }

    private String extractDomain(String url) {
        try {
            String domain = url.replaceFirst("https?://", "").split("/")[0];
            return domain.length() > 30 ? domain.substring(0, 30) : domain;
        } catch (Exception e) {
            return "unknown";
        }
    }
}
