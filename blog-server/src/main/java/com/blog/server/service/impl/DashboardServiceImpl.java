package com.blog.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.server.dto.response.*;
import com.blog.server.entity.*;
import com.blog.server.mapper.*;
import com.blog.server.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final ArticleMapper articleMapper;
    private final CommentMapper commentMapper;
    private final SubscriberMapper subscriberMapper;
    private final VisitLogMapper visitLogMapper;
    private final ArticleLikeMapper articleLikeMapper;

    @Override
    public DashboardSummary getSummary() {
        DashboardSummary summary = new DashboardSummary();

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime weekStart = LocalDate.now().minusDays(6).atStartOfDay();
        LocalDateTime monthStart = LocalDate.now().minusDays(29).atStartOfDay();

        // Views
        summary.setTodayViews(visitLogMapper.selectCount(
                new LambdaQueryWrapper<VisitLog>().ge(VisitLog::getCreatedAt, todayStart)));
        summary.setWeekViews(visitLogMapper.selectCount(
                new LambdaQueryWrapper<VisitLog>().ge(VisitLog::getCreatedAt, weekStart)));
        summary.setMonthViews(visitLogMapper.selectCount(
                new LambdaQueryWrapper<VisitLog>().ge(VisitLog::getCreatedAt, monthStart)));
        summary.setTotalViews(visitLogMapper.selectCount(new LambdaQueryWrapper<>()));

        // UV (unique visitors today by IP)
        summary.setTodayUniqueVisitors(visitLogMapper.countUniqueVisitorsSince(todayStart));

        // Subscribers
        summary.setTotalSubscribers(subscriberMapper.selectCount(
                new LambdaQueryWrapper<Subscriber>().eq(Subscriber::getStatus, "ACTIVE")));
        summary.setNewSubscribers(subscriberMapper.selectCount(
                new LambdaQueryWrapper<Subscriber>()
                        .eq(Subscriber::getStatus, "ACTIVE")
                        .ge(Subscriber::getSubscribedAt, weekStart)));

        // Articles
        summary.setTotalArticles(articleMapper.selectCount(new LambdaQueryWrapper<>()));
        summary.setPublishedArticles(articleMapper.selectCount(
                new LambdaQueryWrapper<Article>().eq(Article::getStatus, "PUBLISHED")));
        summary.setDraftArticles(articleMapper.selectCount(
                new LambdaQueryWrapper<Article>().eq(Article::getStatus, "DRAFT")));

        // Comments
        summary.setTotalComments(commentMapper.selectCount(new LambdaQueryWrapper<>()));
        long pendingComments = commentMapper.selectCount(
                new LambdaQueryWrapper<Comment>().eq(Comment::getStatus, "PENDING"));
        summary.setPendingComments(pendingComments);

        // Likes
        summary.setTotalLikes(articleLikeMapper.selectCount(new LambdaQueryWrapper<>()));

        // Revenue (placeholder)
        summary.setTotalRevenue(0.0);

        // Pending items
        List<DashboardSummary.PendingItem> pendingItems = new ArrayList<>();
        if (pendingComments > 0) {
            pendingItems.add(new DashboardSummary.PendingItem(
                    "comment", "Pending comments", pendingComments, "/comments"));
        }
        long draftCount = summary.getDraftArticles();
        if (draftCount > 0) {
            pendingItems.add(new DashboardSummary.PendingItem(
                    "article", "Draft articles", draftCount, "/articles"));
        }
        summary.setPendingItems(pendingItems);

        return summary;
    }

    @Override
    public List<DashboardTrendPoint> getTrend(int days) {
        if (days <= 0 || days > 90) days = 7;
        LocalDate startDate = LocalDate.now().minusDays(days - 1);

        // Fetch PV and UV per day from the database
        List<Map<String, Object>> pvData = visitLogMapper.selectDailyPV(startDate.atStartOfDay());
        List<Map<String, Object>> uvData = visitLogMapper.selectDailyUV(startDate.atStartOfDay());

        // Map results by date
        Map<String, Long> pvMap = pvData.stream().collect(Collectors.toMap(
                m -> String.valueOf(m.get("day")), m -> ((Number) m.get("cnt")).longValue(),
                (a, b) -> a));
        Map<String, Long> uvMap = uvData.stream().collect(Collectors.toMap(
                m -> String.valueOf(m.get("day")), m -> ((Number) m.get("cnt")).longValue(),
                (a, b) -> a));

        // Fill in all dates
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<DashboardTrendPoint> result = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            String dateStr = startDate.plusDays(i).format(fmt);
            long pv = pvMap.getOrDefault(dateStr, 0L);
            long uv = uvMap.getOrDefault(dateStr, 0L);
            result.add(new DashboardTrendPoint(dateStr, pv, uv));
        }
        return result;
    }

    @Override
    public List<HotArticle> getHotArticles(int limit) {
        if (limit <= 0 || limit > 20) limit = 10;
        return articleMapper.selectHotArticles(limit);
    }

    @Override
    public List<RecentComment> getRecentComments(int limit) {
        if (limit <= 0 || limit > 20) limit = 10;
        return commentMapper.selectRecentWithArticleTitle(limit);
    }
}
