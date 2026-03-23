package com.blog.server.dto.response;

import lombok.Data;

@Data
public class DashboardStats {

    private long totalArticles;
    private long publishedArticles;
    private long totalViews;
    private long totalComments;
    private long totalSubscribers;
    private long totalLikes;
}
