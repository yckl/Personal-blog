package com.blog.server.service;

import com.blog.server.common.PageResult;
import com.blog.server.dto.request.ArticleRequest;
import com.blog.server.dto.response.ArticleVO;

import java.util.List;

public interface ArticleService {

    Long createArticle(ArticleRequest request, Long authorId);

    void updateArticle(Long id, ArticleRequest request);

    void deleteArticle(Long id);

    ArticleVO getArticleById(Long id);

    ArticleVO getArticleBySlug(String slug);

    PageResult<ArticleVO> listArticles(Integer page, Integer size, String status, Long categoryId, Long tagId, String keyword);

    PageResult<ArticleVO> listPublishedArticles(Integer page, Integer size, Long categoryId, Long tagId, String keyword);

    void publishArticle(Long id);

    void unpublishArticle(Long id);

    void incrementViewCount(Long id);

    // ---- Batch operations ----

    void batchDelete(List<Long> ids);

    void batchSetTop(List<Long> ids, boolean isTop);

    void batchArchive(List<Long> ids);

    void batchPublish(List<Long> ids);

    void batchUnpublish(List<Long> ids);

    // ---- Scheduled publishing ----

    void scheduleArticle(Long id, java.time.LocalDateTime scheduledAt);

    /** Called by the scheduler to publish articles whose scheduled time has arrived */
    void publishScheduledArticles();
}
