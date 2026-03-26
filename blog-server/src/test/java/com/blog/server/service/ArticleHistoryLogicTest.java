package com.blog.server.service;

import com.blog.server.dto.request.ArticleRequest;
import com.blog.server.entity.Article;
import com.blog.server.entity.ArticleContent;
import com.blog.server.entity.ArticleHistory;
import com.blog.server.mapper.ArticleContentMapper;
import com.blog.server.mapper.ArticleHistoryMapper;
import com.blog.server.mapper.ArticleMapper;
import com.blog.server.service.impl.ArticleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class ArticleHistoryLogicTest {

    @InjectMocks
    private ArticleServiceImpl articleService;

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private ArticleContentMapper articleContentMapper;

    @Mock
    private ArticleHistoryMapper articleHistoryMapper;

    @Mock
    private EditorService editorService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testUpdateArticle_WithContentChange_ShouldGenerateHistory() {
        Long articleId = 1L;
        ArticleRequest request = new ArticleRequest();
        request.setTitle("New Title");
        request.setContentMd("New updated markdown content.");

        Article existingArticle = new Article();
        existingArticle.setId(articleId);
        existingArticle.setTitle("Old Title");

        ArticleContent existingContent = new ArticleContent();
        existingContent.setArticleId(articleId);
        existingContent.setContentMd("Old markdown content.");

        when(articleMapper.selectById(articleId)).thenReturn(existingArticle);
        when(articleContentMapper.selectOne(any())).thenReturn(existingContent);
        
        // Mock sub-calls to avoid NPE
        when(editorService.generateToc(anyString())).thenReturn(java.util.Collections.emptyList());
        when(editorService.estimateReadingTime(anyString())).thenReturn(5);

        articleService.updateArticle(articleId, request);

        // Verification: Because 'New updated' != 'Old', MUST have called insert()
        verify(articleHistoryMapper, times(1)).insert(any(ArticleHistory.class));
    }

    @Test
    void testUpdateArticle_WithoutContentChange_ShouldNotGenerateHistory() {
        Long articleId = 2L;
        ArticleRequest request = new ArticleRequest();
        request.setTitle("New Title Only");
        request.setContentMd("Same markdown content.");

        Article existingArticle = new Article();
        existingArticle.setId(articleId);
        existingArticle.setTitle("Old Title");

        ArticleContent existingContent = new ArticleContent();
        existingContent.setArticleId(articleId);
        existingContent.setContentMd("Same markdown content.");

        when(articleMapper.selectById(articleId)).thenReturn(existingArticle);
        when(articleContentMapper.selectOne(any())).thenReturn(existingContent);

        when(editorService.generateToc(anyString())).thenReturn(java.util.Collections.emptyList());
        when(editorService.estimateReadingTime(anyString())).thenReturn(5);

        articleService.updateArticle(articleId, request);

        // Verification: Content is identical, insert must NOT trigger
        verify(articleHistoryMapper, never()).insert(any(ArticleHistory.class));
    }
}
