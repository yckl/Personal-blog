package com.blog.server.service;

public interface AiService {

    /** Generate a title from article content */
    String generateTitle(String content, Long articleId, Long userId);

    /** Generate a summary/excerpt */
    String generateSummary(String content, Long articleId, Long userId);

    /** Generate an SEO-optimized title */
    String generateSeoTitle(String content, String currentTitle, Long articleId, Long userId);

    /** Generate a meta description */
    String generateMetaDescription(String content, Long articleId, Long userId);

    /** Generate an article outline from a topic */
    String generateOutline(String topic, Long articleId, Long userId);

    /** Recommend tags based on content */
    String recommendTags(String content, Long articleId, Long userId);

    /** Generate newsletter summary */
    String generateNewsletter(String content, Long articleId, Long userId);

    /** Recommend internal links */
    String recommendInternalLinks(String content, Long articleId, Long userId);
}
