package com.blog.server.service;

import java.util.Map;

public interface PublishService {

    /** Validate and publish an article, optionally triggering newsletter */
    Map<String, Object> publishArticle(Long articleId, boolean sendNewsletter, String visibility, Long userId);

    /** Schedule an article for future publication */
    void scheduleArticle(Long articleId, String scheduledAt, boolean sendNewsletter, String visibility, Long userId);

    /** Generate a time-limited preview link */
    Map<String, Object> generatePreviewLink(Long articleId, Long userId);

    /** Validate a preview token and return article data */
    Map<String, Object> getPreviewByToken(String token);
}
