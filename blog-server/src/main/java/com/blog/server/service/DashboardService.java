package com.blog.server.service;

import com.blog.server.dto.response.DashboardSummary;
import com.blog.server.dto.response.DashboardTrendPoint;
import com.blog.server.dto.response.HotArticle;
import com.blog.server.dto.response.RecentComment;

import java.util.List;

public interface DashboardService {

    DashboardSummary getSummary();

    List<DashboardTrendPoint> getTrend(int days);

    List<HotArticle> getHotArticles(int limit);

    List<RecentComment> getRecentComments(int limit);
}
