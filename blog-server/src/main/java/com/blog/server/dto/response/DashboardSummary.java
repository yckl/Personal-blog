package com.blog.server.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class DashboardSummary {

    private long todayViews;
    private long weekViews;
    private long monthViews;
    private long totalViews;

    private long todayUniqueVisitors;

    private long newSubscribers;
    private long totalSubscribers;

    private long totalArticles;
    private long publishedArticles;
    private long draftArticles;

    private long totalComments;
    private long pendingComments;

    private long totalLikes;

    /** Revenue placeholder */
    private double totalRevenue;

    /** Pending action items */
    private List<PendingItem> pendingItems;

    @Data
    public static class PendingItem {
        private String type;
        private String label;
        private long count;
        private String link;

        public PendingItem(String type, String label, long count, String link) {
            this.type = type;
            this.label = label;
            this.count = count;
            this.link = link;
        }
    }
}
