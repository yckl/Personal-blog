package com.blog.server.dto.response;

import lombok.Data;

@Data
public class DashboardTrendPoint {

    private String date;
    private long pageViews;
    private long uniqueVisitors;

    public DashboardTrendPoint(String date, long pageViews, long uniqueVisitors) {
        this.date = date;
        this.pageViews = pageViews;
        this.uniqueVisitors = uniqueVisitors;
    }
}
