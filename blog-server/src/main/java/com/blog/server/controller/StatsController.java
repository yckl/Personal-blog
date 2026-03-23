package com.blog.server.controller;

import com.blog.server.common.Result;
import com.blog.server.dto.response.DashboardStats;
import com.blog.server.entity.*;
import com.blog.server.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class StatsController {

    private final ArticleMapper articleMapper;
    private final CommentMapper commentMapper;
    private final SubscriberMapper subscriberMapper;
    private final VisitLogMapper visitLogMapper;
    private final ArticleLikeMapper articleLikeMapper;

    @GetMapping("/api/stats/overview")
    public Result<DashboardStats> getOverview() {
        DashboardStats stats = new DashboardStats();
        stats.setTotalArticles(articleMapper.selectCount(new LambdaQueryWrapper<>()));
        stats.setPublishedArticles(articleMapper.selectCount(
                new LambdaQueryWrapper<Article>().eq(Article::getStatus, "PUBLISHED")));
        stats.setTotalComments(commentMapper.selectCount(new LambdaQueryWrapper<>()));
        stats.setTotalSubscribers(subscriberMapper.selectCount(
                new LambdaQueryWrapper<Subscriber>().eq(Subscriber::getStatus, "CONFIRMED")));
        stats.setTotalViews(visitLogMapper.selectCount(new LambdaQueryWrapper<>()));
        stats.setTotalLikes(articleLikeMapper.selectCount(new LambdaQueryWrapper<>()));
        return Result.ok(stats);
    }

    /**
     * Record a page visit with device/browser/OS parsing from User-Agent.
     */
    @PostMapping("/api/visits")
    public Result<Void> recordVisit(@RequestBody VisitRequest request,
                                    HttpServletRequest httpRequest) {
        VisitLog log = new VisitLog();
        log.setArticleId(request.getArticleId());
        log.setPageUrl(request.getPageUrl());
        log.setVisitorId(request.getVisitorId());
        log.setPageType(request.getPageType() != null ? request.getPageType() : "article");
        log.setIpAddress(getClientIp(httpRequest));
        log.setUserAgent(httpRequest.getHeader("User-Agent"));
        log.setReferer(httpRequest.getHeader("Referer"));

        // Parse User-Agent for device/browser/OS
        String ua = httpRequest.getHeader("User-Agent");
        if (ua != null) {
            log.setDeviceType(parseDeviceType(ua));
            log.setBrowser(parseBrowser(ua));
            log.setOs(parseOS(ua));
        }

        visitLogMapper.insert(log);
        return Result.ok();
    }

    /**
     * Update stay duration for a visit (called on page unload).
     */
    @PostMapping("/api/visits/{id}/stay")
    public Result<Void> updateStay(@PathVariable Long id, @RequestBody StayRequest request) {
        visitLogMapper.update(null, new LambdaUpdateWrapper<VisitLog>()
                .eq(VisitLog::getId, id)
                .set(VisitLog::getStaySeconds, request.getSeconds()));
        return Result.ok();
    }

    @PostMapping("/api/articles/{id}/like")
    public Result<Void> likeArticle(@PathVariable Long id, HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        Long count = articleLikeMapper.selectCount(
                new LambdaQueryWrapper<ArticleLike>()
                        .eq(ArticleLike::getArticleId, id)
                        .eq(ArticleLike::getIpAddress, ip));
        if (count == 0) {
            ArticleLike like = new ArticleLike();
            like.setArticleId(id);
            like.setIpAddress(ip);
            articleLikeMapper.insert(like);
            articleMapper.update(null,
                    new LambdaUpdateWrapper<Article>()
                            .eq(Article::getId, id)
                            .setSql("like_count = like_count + 1"));
        }
        return Result.ok();
    }

    // ============ UA Parsing ============

    private String parseDeviceType(String ua) {
        String lower = ua.toLowerCase();
        if (lower.contains("mobile") || lower.contains("android") && !lower.contains("tablet")) return "mobile";
        if (lower.contains("tablet") || lower.contains("ipad")) return "tablet";
        return "desktop";
    }

    private String parseBrowser(String ua) {
        if (ua.contains("Edg/")) return "Edge";
        if (ua.contains("OPR/") || ua.contains("Opera")) return "Opera";
        if (ua.contains("Chrome/") && !ua.contains("Edg/")) return "Chrome";
        if (ua.contains("Firefox/")) return "Firefox";
        if (ua.contains("Safari/") && !ua.contains("Chrome/")) return "Safari";
        if (ua.contains("MSIE") || ua.contains("Trident")) return "IE";
        return "Other";
    }

    private String parseOS(String ua) {
        if (ua.contains("Windows")) return "Windows";
        if (ua.contains("Mac OS X") || ua.contains("Macintosh")) return "macOS";
        if (ua.contains("Linux") && !ua.contains("Android")) return "Linux";
        if (ua.contains("Android")) return "Android";
        if (ua.contains("iPhone") || ua.contains("iPad") || ua.contains("iPod")) return "iOS";
        if (ua.contains("CrOS")) return "ChromeOS";
        return "Other";
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwarded = request.getHeader("X-Forwarded-For");
        if (xForwarded != null && !xForwarded.isEmpty()) {
            return xForwarded.split(",")[0].trim();
        }
        String xReal = request.getHeader("X-Real-IP");
        if (xReal != null && !xReal.isEmpty()) return xReal;
        return request.getRemoteAddr();
    }

    @Data
    public static class VisitRequest {
        private Long articleId;
        private String pageUrl;
        private String visitorId;
        private String pageType;
    }

    @Data
    public static class StayRequest {
        private Integer seconds;
    }
}
