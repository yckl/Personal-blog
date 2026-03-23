package com.blog.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.server.common.PageResult;
import com.blog.server.dto.request.ArticleRequest;
import com.blog.server.dto.response.ArticleVO;
import com.blog.server.entity.*;
import com.blog.server.exception.BusinessException;
import com.blog.server.mapper.*;
import com.blog.server.service.ArticleService;
import com.blog.server.service.EditorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private final ArticleMapper articleMapper;
    private final ArticleContentMapper articleContentMapper;
    private final ArticleCategoryMapper categoryMapper;
    private final ArticleTagMapper tagMapper;
    private final ArticleTagRelMapper tagRelMapper;
    private final ArticleVersionMapper versionMapper;
    private final ArticleSeriesRelMapper seriesRelMapper;
    private final SysUserMapper userMapper;
    private final EditorService editorService;

    @Override
    @Transactional
    public Long createArticle(ArticleRequest request, Long authorId) {
        Article article = new Article();
        article.setTitle(request.getTitle());
        article.setSlug(generateSlug(request.getSlug(), request.getTitle()));
        article.setExcerpt(request.getExcerpt());
        article.setCoverImage(request.getCoverImage());
        article.setAuthorId(authorId);
        article.setCategoryId(request.getCategoryId());
        article.setStatus(request.getStatus() != null ? request.getStatus() : "DRAFT");
        article.setIsTop(request.getIsTop() != null ? request.getIsTop() : false);
        article.setIsFeatured(request.getIsFeatured() != null ? request.getIsFeatured() : false);
        article.setAllowComment(request.getAllowComment() != null ? request.getAllowComment() : true);
        article.setViewCount(0);
        article.setLikeCount(0);
        article.setCommentCount(0);
        article.setWordCount(countWords(request.getContentMd()));

        if ("PUBLISHED".equals(article.getStatus())) {
            article.setPublishedAt(LocalDateTime.now());
        } else if ("SCHEDULED".equals(article.getStatus()) && request.getScheduledAt() != null) {
            article.setScheduledAt(request.getScheduledAt());
        }

        articleMapper.insert(article);

        // Save content with TOC and reading time
        ArticleContent content = new ArticleContent();
        content.setArticleId(article.getId());
        content.setContentMd(request.getContentMd());
        content.setContentHtml(request.getContentHtml());
        try {
            content.setTocJson(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(
                    editorService.generateToc(request.getContentMd())));
        } catch (Exception e) { content.setTocJson("[]"); }
        content.setReadingTime(editorService.estimateReadingTime(request.getContentMd()));
        articleContentMapper.insert(content);

        // Save tags
        saveArticleTags(article.getId(), request.getTagIds());

        // Save series relation
        if (request.getSeriesId() != null) {
            ArticleSeriesRel rel = new ArticleSeriesRel();
            rel.setArticleId(article.getId());
            rel.setSeriesId(request.getSeriesId());
            rel.setSortOrder(request.getSeriesOrder() != null ? request.getSeriesOrder() : 0);
            seriesRelMapper.insert(rel);
        }

        // Save version
        saveVersion(article.getId(), article.getTitle(), request.getContentMd(), authorId);

        return article.getId();
    }

    @Override
    @Transactional
    public void updateArticle(Long id, ArticleRequest request) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException(404, "Article not found");
        }

        article.setTitle(request.getTitle());
        if (StringUtils.hasText(request.getSlug())) {
            article.setSlug(request.getSlug());
        }
        article.setExcerpt(request.getExcerpt());
        article.setCoverImage(request.getCoverImage());
        article.setCategoryId(request.getCategoryId());
        if (request.getIsTop() != null) article.setIsTop(request.getIsTop());
        if (request.getIsFeatured() != null) article.setIsFeatured(request.getIsFeatured());
        if (request.getAllowComment() != null) article.setAllowComment(request.getAllowComment());
        article.setWordCount(countWords(request.getContentMd()));

        if (request.getStatus() != null) {
            if ("PUBLISHED".equals(request.getStatus()) && !"PUBLISHED".equals(article.getStatus())) {
                article.setPublishedAt(LocalDateTime.now());
            }
            article.setStatus(request.getStatus());
        }

        articleMapper.updateById(article);

        // Update content
        ArticleContent content = articleContentMapper.selectOne(
                new LambdaQueryWrapper<ArticleContent>().eq(ArticleContent::getArticleId, id)
        );
        if (content != null) {
            content.setContentMd(request.getContentMd());
            content.setContentHtml(request.getContentHtml());
            try {
                content.setTocJson(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(
                        editorService.generateToc(request.getContentMd())));
            } catch (Exception e) { content.setTocJson("[]"); }
            content.setReadingTime(editorService.estimateReadingTime(request.getContentMd()));
            articleContentMapper.updateById(content);
        }

        // Update tags
        tagRelMapper.delete(new LambdaQueryWrapper<ArticleTagRel>().eq(ArticleTagRel::getArticleId, id));
        saveArticleTags(id, request.getTagIds());

        // Update series
        seriesRelMapper.delete(new LambdaQueryWrapper<ArticleSeriesRel>().eq(ArticleSeriesRel::getArticleId, id));
        if (request.getSeriesId() != null) {
            ArticleSeriesRel rel = new ArticleSeriesRel();
            rel.setArticleId(id);
            rel.setSeriesId(request.getSeriesId());
            rel.setSortOrder(request.getSeriesOrder() != null ? request.getSeriesOrder() : 0);
            seriesRelMapper.insert(rel);
        }

        // Save version
        saveVersion(id, article.getTitle(), request.getContentMd(), article.getAuthorId());
    }

    @Override
    public void deleteArticle(Long id) {
        articleMapper.deleteById(id);
    }

    @Override
    public ArticleVO getArticleById(Long id) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException(404, "Article not found");
        }
        return convertToVO(article, true);
    }

    @Override
    public ArticleVO getArticleBySlug(String slug) {
        Article article = articleMapper.selectOne(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getSlug, slug)
                        .eq(Article::getStatus, "PUBLISHED")
        );
        if (article == null) {
            throw new BusinessException(404, "Article not found");
        }
        return convertToVO(article, true);
    }

    @Override
    public PageResult<ArticleVO> listArticles(Integer page, Integer size, String status,
                                               Long categoryId, Long tagId, String keyword) {
        return doListArticles(page, size, status, categoryId, tagId, keyword);
    }

    @Override
    public PageResult<ArticleVO> listPublishedArticles(Integer page, Integer size,
                                                        Long categoryId, Long tagId, String keyword) {
        return doListArticles(page, size, "PUBLISHED", categoryId, tagId, keyword);
    }

    private PageResult<ArticleVO> doListArticles(Integer page, Integer size, String status,
                                                  Long categoryId, Long tagId, String keyword) {
        Page<Article> pageParam = new Page<>(page != null ? page : 1, size != null ? size : 10);

        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(Article::getStatus, status);
        }
        if (categoryId != null) {
            wrapper.eq(Article::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Article::getTitle, keyword).or().like(Article::getExcerpt, keyword));
        }

        // If filtering by tag, get article IDs first
        if (tagId != null) {
            List<ArticleTagRel> rels = tagRelMapper.selectList(
                    new LambdaQueryWrapper<ArticleTagRel>().eq(ArticleTagRel::getTagId, tagId)
            );
            List<Long> articleIds = rels.stream().map(ArticleTagRel::getArticleId).toList();
            if (articleIds.isEmpty()) {
                return new PageResult<>(new ArrayList<>(), 0, page != null ? page : 1, size != null ? size : 10);
            }
            wrapper.in(Article::getId, articleIds);
        }

        wrapper.orderByDesc(Article::getIsTop).orderByDesc(Article::getCreatedAt);

        Page<Article> result = articleMapper.selectPage(pageParam, wrapper);

        List<ArticleVO> records = result.getRecords().stream()
                .map(a -> convertToVO(a, false))
                .collect(Collectors.toList());

        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public void publishArticle(Long id) {
        articleMapper.update(null,
                new LambdaUpdateWrapper<Article>()
                        .eq(Article::getId, id)
                        .set(Article::getStatus, "PUBLISHED")
                        .set(Article::getPublishedAt, LocalDateTime.now())
        );
    }

    @Override
    public void unpublishArticle(Long id) {
        articleMapper.update(null,
                new LambdaUpdateWrapper<Article>()
                        .eq(Article::getId, id)
                        .set(Article::getStatus, "DRAFT")
        );
    }

    @Override
    public void incrementViewCount(Long id) {
        articleMapper.update(null,
                new LambdaUpdateWrapper<Article>()
                        .eq(Article::getId, id)
                        .setSql("view_count = view_count + 1")
        );
    }

    // ---- Batch operations ----

    @Override
    @Transactional
    public void batchDelete(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            articleMapper.deleteBatchIds(ids);
        }
    }

    @Override
    @Transactional
    public void batchSetTop(List<Long> ids, boolean isTop) {
        if (ids != null && !ids.isEmpty()) {
            articleMapper.update(null,
                    new LambdaUpdateWrapper<Article>()
                            .in(Article::getId, ids)
                            .set(Article::getIsTop, isTop)
            );
        }
    }

    @Override
    @Transactional
    public void batchArchive(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            articleMapper.update(null,
                    new LambdaUpdateWrapper<Article>()
                            .in(Article::getId, ids)
                            .set(Article::getStatus, "ARCHIVED")
            );
        }
    }

    @Override
    @Transactional
    public void batchPublish(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            articleMapper.update(null,
                    new LambdaUpdateWrapper<Article>()
                            .in(Article::getId, ids)
                            .set(Article::getStatus, "PUBLISHED")
                            .set(Article::getPublishedAt, LocalDateTime.now())
            );
        }
    }

    @Override
    @Transactional
    public void batchUnpublish(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            articleMapper.update(null,
                    new LambdaUpdateWrapper<Article>()
                            .in(Article::getId, ids)
                            .set(Article::getStatus, "DRAFT")
            );
        }
    }

    // ---- Scheduled publishing ----

    @Override
    public void scheduleArticle(Long id, LocalDateTime scheduledAt) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException(404, "Article not found");
        }
        articleMapper.update(null,
                new LambdaUpdateWrapper<Article>()
                        .eq(Article::getId, id)
                        .set(Article::getStatus, "SCHEDULED")
                        .set(Article::getScheduledAt, scheduledAt)
        );
    }

    @Override
    @Transactional
    public void publishScheduledArticles() {
        List<Article> scheduled = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getStatus, "SCHEDULED")
                        .le(Article::getScheduledAt, LocalDateTime.now())
        );
        for (Article article : scheduled) {
            article.setStatus("PUBLISHED");
            article.setPublishedAt(LocalDateTime.now());
            articleMapper.updateById(article);
        }
    }

    // ---- Helpers ----

    private ArticleVO convertToVO(Article article, boolean includeContent) {
        ArticleVO vo = new ArticleVO();
        vo.setId(article.getId());
        vo.setTitle(article.getTitle());
        vo.setSlug(article.getSlug());
        vo.setExcerpt(article.getExcerpt());
        vo.setCoverImage(article.getCoverImage());
        vo.setStatus(article.getStatus());
        vo.setIsTop(article.getIsTop());
        vo.setIsFeatured(article.getIsFeatured());
        vo.setAllowComment(article.getAllowComment());
        vo.setViewCount(article.getViewCount());
        vo.setLikeCount(article.getLikeCount());
        vo.setCommentCount(article.getCommentCount());
        vo.setWordCount(article.getWordCount());
        vo.setPublishedAt(article.getPublishedAt());
        vo.setCreatedAt(article.getCreatedAt());
        vo.setUpdatedAt(article.getUpdatedAt());
        vo.setCategoryId(article.getCategoryId());

        // Author
        SysUser author = userMapper.selectById(article.getAuthorId());
        if (author != null) {
            vo.setAuthorName(author.getNickname() != null ? author.getNickname() : author.getUsername());
            vo.setAuthorAvatar(author.getAvatar());
        }

        // Category
        if (article.getCategoryId() != null) {
            ArticleCategory category = categoryMapper.selectById(article.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getName());
            }
        }

        // Tags
        List<ArticleTagRel> tagRels = tagRelMapper.selectList(
                new LambdaQueryWrapper<ArticleTagRel>().eq(ArticleTagRel::getArticleId, article.getId())
        );
        List<ArticleVO.TagVO> tags = tagRels.stream().map(rel -> {
            ArticleTag tag = tagMapper.selectById(rel.getTagId());
            if (tag != null) {
                ArticleVO.TagVO tagVO = new ArticleVO.TagVO();
                tagVO.setId(tag.getId());
                tagVO.setName(tag.getName());
                tagVO.setSlug(tag.getSlug());
                tagVO.setColor(tag.getColor());
                return tagVO;
            }
            return null;
        }).filter(t -> t != null).collect(Collectors.toList());
        vo.setTags(tags);

        // Content (detail view only)
        if (includeContent) {
            ArticleContent content = articleContentMapper.selectOne(
                    new LambdaQueryWrapper<ArticleContent>().eq(ArticleContent::getArticleId, article.getId())
            );
            if (content != null) {
                vo.setContentMd(content.getContentMd());
                vo.setContentHtml(content.getContentHtml());
                vo.setTocJson(content.getTocJson());
            }
            // Reading time: ~200 words per minute
            int wc = article.getWordCount() != null ? article.getWordCount() : 0;
            vo.setReadingTime(Math.max(1, wc / 200));
        }

        return vo;
    }

    private void saveArticleTags(Long articleId, List<Long> tagIds) {
        if (tagIds != null && !tagIds.isEmpty()) {
            for (Long tagId : tagIds) {
                ArticleTagRel rel = new ArticleTagRel();
                rel.setArticleId(articleId);
                rel.setTagId(tagId);
                tagRelMapper.insert(rel);
            }
        }
    }

    private void saveVersion(Long articleId, String title, String contentMd, Long userId) {
        // Get max version number
        Long count = versionMapper.selectCount(
                new LambdaQueryWrapper<ArticleVersion>().eq(ArticleVersion::getArticleId, articleId)
        );
        ArticleVersion version = new ArticleVersion();
        version.setArticleId(articleId);
        version.setTitle(title);
        version.setContentMd(contentMd);
        version.setVersionNum(count.intValue() + 1);
        version.setCreatedBy(userId);
        versionMapper.insert(version);
    }

    private String generateSlug(String slug, String title) {
        if (StringUtils.hasText(slug)) {
            return slug;
        }
        // Simple slug generation from title
        return title.toLowerCase()
                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-")
                .replaceAll("^-|-$", "")
                + "-" + System.currentTimeMillis() % 10000;
    }

    private int countWords(String content) {
        if (content == null || content.isEmpty()) return 0;
        // Count Chinese characters + English words
        String cleaned = content.replaceAll("[#*`\\[\\]()>!\\-]", "").trim();
        int chineseCount = cleaned.replaceAll("[^\\u4e00-\\u9fa5]", "").length();
        int englishWords = cleaned.replaceAll("[\\u4e00-\\u9fa5]", "").trim().split("\\s+").length;
        return chineseCount + englishWords;
    }
}
