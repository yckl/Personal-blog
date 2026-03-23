package com.blog.server.service;

import java.util.List;
import java.util.Map;

public interface EditorService {

    /** Generate TOC tree from markdown content */
    List<Map<String, Object>> generateToc(String markdownContent);

    /** Estimate reading time in minutes */
    int estimateReadingTime(String markdownContent);

    /** Count words (Chinese + English) */
    int countWords(String markdownContent);

    /** Save draft (auto-save) — creates or updates article in DRAFT status */
    Long saveDraft(Long articleId, String title, String contentMd, String contentHtml, Long userId);
}
