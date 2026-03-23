package com.blog.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.server.entity.Article;
import com.blog.server.entity.ArticleContent;
import com.blog.server.entity.ArticleVersion;
import com.blog.server.exception.BusinessException;
import com.blog.server.mapper.ArticleContentMapper;
import com.blog.server.mapper.ArticleMapper;
import com.blog.server.mapper.ArticleVersionMapper;
import com.blog.server.service.VersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VersionServiceImpl implements VersionService {

    private final ArticleVersionMapper versionMapper;
    private final ArticleMapper articleMapper;
    private final ArticleContentMapper contentMapper;

    @Override
    public List<ArticleVersion> listVersions(Long articleId) {
        return versionMapper.selectList(
                new LambdaQueryWrapper<ArticleVersion>()
                        .eq(ArticleVersion::getArticleId, articleId)
                        .orderByDesc(ArticleVersion::getVersionNum)
        );
    }

    @Override
    public ArticleVersion getVersion(Long versionId) {
        ArticleVersion v = versionMapper.selectById(versionId);
        if (v == null) throw new BusinessException(404, "Version not found");
        return v;
    }

    @Override
    @Transactional
    public void rollbackToVersion(Long articleId, Long versionId) {
        ArticleVersion version = versionMapper.selectById(versionId);
        if (version == null || !version.getArticleId().equals(articleId)) {
            throw new BusinessException(404, "Version not found for this article");
        }

        // Update article title
        Article article = articleMapper.selectById(articleId);
        if (article == null) throw new BusinessException(404, "Article not found");
        article.setTitle(version.getTitle());
        articleMapper.updateById(article);

        // Update article content
        ArticleContent content = contentMapper.selectOne(
                new LambdaQueryWrapper<ArticleContent>().eq(ArticleContent::getArticleId, articleId)
        );
        if (content != null) {
            content.setContentMd(version.getContentMd());
            contentMapper.updateById(content);
        }
    }
}
