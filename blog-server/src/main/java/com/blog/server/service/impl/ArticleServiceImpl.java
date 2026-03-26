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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
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
    private final com.blog.server.mapper.ArticleHistoryMapper articleHistoryMapper;

    @Override
    @Transactional
    @CacheEvict(value = "public_articles", allEntries = true)
    public Long createArticle(ArticleRequest request, Long authorId) {
        // Validate status
        String status = request.getStatus() != null ? request.getStatus() : "DRAFT";
        validateStatus(status);

        Article article = new Article();
        article.setTitle(request.getTitle());
        article.setSlug(generateUniqueSlug(request.getSlug(), request.getTitle(), null));
        article.setExcerpt(request.getExcerpt());
        article.setCoverImage(request.getCoverImage());
        article.setAuthorId(authorId);
        article.setCategoryId(request.getCategoryId());
        article.setStatus(status);
        article.setIsTop(request.getIsTop() != null ? request.getIsTop() : false);
        article.setIsFeatured(request.getIsFeatured() != null ? request.getIsFeatured() : false);
        article.setAllowComment(request.getAllowComment() != null ? request.getAllowComment() : true);
        article.setViewCount(0);
        article.setLikeCount(0);
        article.setCommentCount(0);
        article.setWordCount(countWords(request.getContentMd()));

        if ("PUBLISHED".equals(status)) {
            article.setPublishedAt(LocalDateTime.now());
        } else if ("SCHEDULED".equals(status) && request.getScheduledAt() != null) {
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
    @CacheEvict(value = "public_articles", allEntries = true)
    public void updateArticle(Long id, ArticleRequest request) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException(404, "Article not found");
        }

        article.setTitle(request.getTitle());
        if (StringUtils.hasText(request.getSlug())) {
            // Validate slug uniqueness (excluding current article)
            article.setSlug(generateUniqueSlug(request.getSlug(), null, id));
        }
        article.setExcerpt(request.getExcerpt());
        article.setCoverImage(request.getCoverImage());
        article.setCategoryId(request.getCategoryId());
        if (request.getIsTop() != null) article.setIsTop(request.getIsTop());
        if (request.getIsFeatured() != null) article.setIsFeatured(request.getIsFeatured());
        if (request.getAllowComment() != null) article.setAllowComment(request.getAllowComment());
        article.setWordCount(countWords(request.getContentMd()));

        if (request.getStatus() != null) {
            validateStatus(request.getStatus());
            if ("PUBLISHED".equals(request.getStatus()) && !"PUBLISHED".equals(article.getStatus())) {
                article.setPublishedAt(LocalDateTime.now());
            }
            // Clear scheduledAt when transitioning away from SCHEDULED
            if (!"SCHEDULED".equals(request.getStatus())) {
                article.setScheduledAt(null);
            }
            article.setStatus(request.getStatus());
        }

        articleMapper.updateById(article);

        // Update content
        ArticleContent content = articleContentMapper.selectOne(
                new LambdaQueryWrapper<ArticleContent>().eq(ArticleContent::getArticleId, id)
        );
        if (content != null) {
            String oldContent = content.getContentMd() != null ? content.getContentMd() : "";
            String newContent = request.getContentMd() != null ? request.getContentMd() : "";
            if (!oldContent.equals(newContent)) {
                com.blog.server.entity.ArticleHistory history = new com.blog.server.entity.ArticleHistory();
                history.setArticleId(id);
                history.setTitle(request.getTitle() != null ? request.getTitle() : article.getTitle());
                history.setContentMd(newContent);
                history.setVersionHash(org.springframework.util.DigestUtils.md5DigestAsHex(newContent.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                articleHistoryMapper.insert(history);
            }

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
    @CacheEvict(value = "public_articles", allEntries = true)
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
    @Cacheable(value = "public_articles", key = "'slug_' + #slug")
    public ArticleVO getArticleBySlug(String slug) {
        Article article = articleMapper.selectOne(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getSlug, slug)
                        .eq(Article::getStatus, "PUBLISHED")
                        .last("LIMIT 1")
        );
        if (article == null) {
            throw new BusinessException(404, "Article not found");
        }
        return convertToVO(article, true);
    }

    @Override
    public PageResult<ArticleVO> listArticles(Integer page, Integer size, String status,
                                               Long categoryId, Long tagId, String keyword, String sort) {
        return doListArticles(page, size, status, categoryId, tagId, keyword, sort);
    }

    @Override
    @Cacheable(value = "public_articles", key = "'list_' + (#page ?: 1) + '_' + (#size ?: 10) + '_' + (#categoryId ?: 0) + '_' + (#tagId ?: 0) + '_' + (#keyword ?: '') + '_' + (#sort ?: '')")
    public PageResult<ArticleVO> listPublishedArticles(Integer page, Integer size,
                                                        Long categoryId, Long tagId, String keyword, String sort) {
        return doListArticles(page, size, "PUBLISHED", categoryId, tagId, keyword, sort);
    }

    private PageResult<ArticleVO> doListArticles(Integer page, Integer size, String status,
                                                  Long categoryId, Long tagId, String keyword, String sort) {
        int p = page != null ? page : 1;
        int s = Math.min(size != null ? size : 10, 100); // Cap at 100
        Page<Article> pageParam = new Page<>(p, s);

        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(Article::getStatus, status);
        }
        if (categoryId != null) {
            wrapper.eq(Article::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(keyword)) {
            // Split-character fuzzy matching: e.g. "Spr" -> "%S%p%r%"
            String fuzzyKeyword = "%" + String.join("%", keyword.split("")) + "%";
            wrapper.and(w -> w.like(Article::getTitle, fuzzyKeyword).or().like(Article::getExcerpt, fuzzyKeyword));
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

        // Apply sorting rules
        if ("hot".equals(sort)) {
            wrapper.orderByDesc(Article::getIsTop).orderByDesc(Article::getViewCount).orderByDesc(Article::getCreatedAt);
        } else if ("recommended".equals(sort)) {
            wrapper.orderByDesc(Article::getIsFeatured).orderByDesc(Article::getIsTop).orderByDesc(Article::getCreatedAt);
        } else {
            // Default "latest"
            wrapper.orderByDesc(Article::getIsTop).orderByDesc(Article::getCreatedAt);
        }

        Page<Article> result = articleMapper.selectPage(pageParam, wrapper);

        List<ArticleVO> records = result.getRecords().stream()
                .map(a -> convertToVO(a, false))
                .collect(Collectors.toList());

        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @CacheEvict(value = "public_articles", allEntries = true)
    public void publishArticle(Long id) {
        articleMapper.update(null,
                new LambdaUpdateWrapper<Article>()
                        .eq(Article::getId, id)
                        .set(Article::getStatus, "PUBLISHED")
                        .set(Article::getPublishedAt, LocalDateTime.now())
        );
    }

    @Override
    @CacheEvict(value = "public_articles", allEntries = true)
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
    @CacheEvict(value = "public_articles", allEntries = true)
    public void batchDelete(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            if (ids.size() > 100) throw new BusinessException(400, "Batch operations limited to 100 items");
            articleMapper.deleteBatchIds(ids);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "public_articles", allEntries = true)
    public void batchSetTop(List<Long> ids, boolean isTop) {
        if (ids != null && !ids.isEmpty()) {
            if (ids.size() > 100) throw new BusinessException(400, "Batch operations limited to 100 items");
            articleMapper.update(null,
                    new LambdaUpdateWrapper<Article>()
                            .in(Article::getId, ids)
                            .set(Article::getIsTop, isTop)
            );
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "public_articles", allEntries = true)
    public void batchArchive(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            if (ids.size() > 100) throw new BusinessException(400, "Batch operations limited to 100 items");
            articleMapper.update(null,
                    new LambdaUpdateWrapper<Article>()
                            .in(Article::getId, ids)
                            .set(Article::getStatus, "ARCHIVED")
            );
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "public_articles", allEntries = true)
    public void batchPublish(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            if (ids.size() > 100) throw new BusinessException(400, "Batch operations limited to 100 items");
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
    @CacheEvict(value = "public_articles", allEntries = true)
    public void batchUnpublish(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            if (ids.size() > 100) throw new BusinessException(400, "Batch operations limited to 100 items");
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
    @CacheEvict(value = "public_articles", allEntries = true)
    public void publishScheduledArticles() {
        List<Article> scheduled = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getStatus, "SCHEDULED")
                        .le(Article::getScheduledAt, LocalDateTime.now())
        );
        for (Article article : scheduled) {
            article.setStatus("PUBLISHED");
            article.setPublishedAt(LocalDateTime.now());
            article.setScheduledAt(null);  // Clear scheduled time
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
                    new LambdaQueryWrapper<ArticleContent>()
                            .eq(ArticleContent::getArticleId, article.getId())
                            .last("LIMIT 1")
            );
            if (content != null) {
                vo.setContentMd(content.getContentMd());
                vo.setContentHtml(content.getContentHtml());
                vo.setTocJson(content.getTocJson());
            }
        }

        // Reading time: ~200 words per minute (always included)
        int wc = article.getWordCount() != null ? article.getWordCount() : 0;
        vo.setReadingTime(Math.max(1, wc / 200));

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

    private static final Set<String> VALID_STATUSES = Set.of(
            "DRAFT", "PUBLISHED", "SCHEDULED", "ARCHIVED");

    private void validateStatus(String status) {
        if (!VALID_STATUSES.contains(status)) {
            throw new BusinessException(400, "Invalid article status: " + status
                    + ". Must be one of: " + String.join(", ", VALID_STATUSES));
        }
    }

    /**
     * Generate a unique slug. If the desired slug already exists (for a different article),
     * append a numeric suffix.
     * @param slug explicit slug from user (may be null)
     * @param title article title (used to generate slug if slug is null)
     * @param excludeId article ID to exclude from uniqueness check (for updates), may be null
     */
    private String generateUniqueSlug(String slug, String title, Long excludeId) {
        String base;
        if (StringUtils.hasText(slug)) {
            base = slug;
        } else {
            base = title.toLowerCase()
                    .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-")
                    .replaceAll("^-|-$", "");
        }

        String candidate = base;
        int suffix = 1;
        while (true) {
            LambdaQueryWrapper<Article> check = new LambdaQueryWrapper<Article>()
                    .eq(Article::getSlug, candidate);
            if (excludeId != null) {
                check.ne(Article::getId, excludeId);
            }
            if (articleMapper.selectCount(check) == 0) {
                return candidate;
            }
            candidate = base + "-" + suffix;
            suffix++;
            if (suffix > 100) {
                // Safety: prevent infinite loop
                return base + "-" + System.currentTimeMillis();
            }
        }
    }

    private int countWords(String content) {
        if (content == null || content.isEmpty()) return 0;
        // Count Chinese characters + English words
        String cleaned = content.replaceAll("[#*`\\[\\]()>!\\-]", "").trim();
        int chineseCount = cleaned.replaceAll("[^\\u4e00-\\u9fa5]", "").length();
        int englishWords = cleaned.replaceAll("[\\u4e00-\\u9fa5]", "").trim().split("\\s+").length;
        return chineseCount + englishWords;
    }

    // ---- Recycle Bin ----

    @Override
    public PageResult<ArticleVO> listDeletedArticles(Integer page, Integer size) {
        // MyBatis-Plus @TableLogic filters deleted=1 by default, so we use a raw wrapper
        int p = page != null ? page : 1;
        int s = size != null ? size : 20;
        Page<Article> pageParam = new Page<>(p, s);

        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<Article>()
                .apply("deleted = 1")  // bypass @TableLogic
                .orderByDesc(Article::getUpdatedAt);

        Page<Article> result = articleMapper.selectPage(pageParam, wrapper);
        List<ArticleVO> records = result.getRecords().stream()
                .map(a -> convertToVO(a, false))
                .collect(Collectors.toList());
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional
    public void restoreArticle(Long id) {
        // Bypass @TableLogic — directly set deleted = 0
        articleMapper.update(null,
                new LambdaUpdateWrapper<Article>()
                        .apply("deleted = 1")
                        .eq(Article::getId, id)
                        .set(Article::getDeleted, 0)
                        .set(Article::getStatus, "DRAFT"));
    }

    @Override
    @Transactional
    public void permanentDeleteArticle(Long id) {
        // Hard delete — remove article and all related records
        articleContentMapper.delete(
                new LambdaQueryWrapper<ArticleContent>().eq(ArticleContent::getArticleId, id));
        tagRelMapper.delete(
                new LambdaQueryWrapper<ArticleTagRel>().eq(ArticleTagRel::getArticleId, id));
        seriesRelMapper.delete(
                new LambdaQueryWrapper<ArticleSeriesRel>().eq(ArticleSeriesRel::getArticleId, id));
        versionMapper.delete(
                new LambdaQueryWrapper<ArticleVersion>().eq(ArticleVersion::getArticleId, id));
        // Hard delete the article itself (bypass @TableLogic)
        articleMapper.delete(
                new LambdaQueryWrapper<Article>()
                        .apply("deleted = 1")
                        .eq(Article::getId, id));
    }
}
