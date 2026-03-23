package com.blog.server.service;

import java.util.List;
import java.util.Map;

public interface ContentOrganizationService {

    /** Get categories as a tree structure with article counts */
    List<Map<String, Object>> getCategoryTree();

    /** Get articles by category slug */
    List<Map<String, Object>> getArticlesByCategory(String slug);

    /** Get all tags with article counts */
    List<Map<String, Object>> getTagsWithCount();

    /** Get articles by tag slug */
    List<Map<String, Object>> getArticlesByTag(String slug);

    /** Get all series with article counts */
    List<Map<String, Object>> getSeriesWithCount();

    /** Get series detail with ordered articles */
    Map<String, Object> getSeriesDetail(String slug);

    /** Get archive grouped by year-month */
    List<Map<String, Object>> getArchive();

    /** Get archive articles by year and optional month */
    List<Map<String, Object>> getArchiveArticles(int year, Integer month);
}
