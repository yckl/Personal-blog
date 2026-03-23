package com.blog.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.server.entity.Article;
import com.blog.server.entity.ArticleContent;
import com.blog.server.mapper.ArticleContentMapper;
import com.blog.server.mapper.ArticleMapper;
import com.blog.server.service.EditorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class EditorServiceImpl implements EditorService {

    private final ArticleMapper articleMapper;
    private final ArticleContentMapper contentMapper;
    private final ObjectMapper objectMapper;

    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$", Pattern.MULTILINE);

    @Override
    public List<Map<String, Object>> generateToc(String markdownContent) {
        List<Map<String, Object>> toc = new ArrayList<>();
        if (markdownContent == null || markdownContent.isEmpty()) return toc;

        Matcher matcher = HEADING_PATTERN.matcher(markdownContent);
        while (matcher.find()) {
            int level = matcher.group(1).length();
            String text = matcher.group(2).trim();
            // Strip markdown inline formatting
            text = text.replaceAll("[*_`~]", "");
            String anchor = text.toLowerCase()
                    .replaceAll("[^a-z0-9\\u4e00-\\u9fa5\\s-]", "")
                    .replaceAll("\\s+", "-");

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("level", level);
            item.put("text", text);
            item.put("anchor", anchor);
            toc.add(item);
        }
        return toc;
    }

    @Override
    public int estimateReadingTime(String markdownContent) {
        if (markdownContent == null || markdownContent.isEmpty()) return 0;
        // Strip markdown syntax
        String cleaned = markdownContent
                .replaceAll("```[\\s\\S]*?```", "")  // code blocks
                .replaceAll("`[^`]+`", "")             // inline code
                .replaceAll("!?\\[.*?]\\(.*?\\)", "")  // links and images
                .replaceAll("[#*_>~`|\\-]", "")        // markdown chars
                .trim();

        // Count Chinese characters (300 chars/min)
        int chineseCount = cleaned.replaceAll("[^\\u4e00-\\u9fa5]", "").length();
        // Count English words (200 words/min)
        String englishOnly = cleaned.replaceAll("[\\u4e00-\\u9fa5]", "").trim();
        int englishWords = englishOnly.isEmpty() ? 0 : englishOnly.split("\\s+").length;

        double minutes = (double) chineseCount / 350 + (double) englishWords / 225;
        return Math.max(1, (int) Math.ceil(minutes));
    }

    @Override
    public int countWords(String markdownContent) {
        if (markdownContent == null || markdownContent.isEmpty()) return 0;
        String cleaned = markdownContent
                .replaceAll("```[\\s\\S]*?```", "")
                .replaceAll("[#*`\\[\\]()>!\\-]", "")
                .trim();
        int chineseCount = cleaned.replaceAll("[^\\u4e00-\\u9fa5]", "").length();
        int englishWords = cleaned.replaceAll("[\\u4e00-\\u9fa5]", "").trim().isEmpty()
                ? 0 : cleaned.replaceAll("[\\u4e00-\\u9fa5]", "").trim().split("\\s+").length;
        return chineseCount + englishWords;
    }

    @Override
    @Transactional
    public Long saveDraft(Long articleId, String title, String contentMd, String contentHtml, Long userId) {
        List<Map<String, Object>> toc = generateToc(contentMd);
        int readingTime = estimateReadingTime(contentMd);
        int wordCount = countWords(contentMd);
        String tocJson;
        try {
            tocJson = objectMapper.writeValueAsString(toc);
        } catch (Exception e) {
            tocJson = "[]";
        }

        if (articleId != null) {
            // Update existing
            Article article = articleMapper.selectById(articleId);
            if (article != null) {
                article.setTitle(title);
                article.setWordCount(wordCount);
                articleMapper.updateById(article);

                ArticleContent content = contentMapper.selectOne(
                        new LambdaQueryWrapper<ArticleContent>().eq(ArticleContent::getArticleId, articleId));
                if (content != null) {
                    content.setContentMd(contentMd);
                    content.setContentHtml(contentHtml);
                    content.setTocJson(tocJson);
                    content.setReadingTime(readingTime);
                    contentMapper.updateById(content);
                }
                return articleId;
            }
        }

        // Create new draft
        Article article = new Article();
        article.setTitle(title != null && !title.isEmpty() ? title : "Untitled Draft");
        article.setSlug("draft-" + System.currentTimeMillis());
        article.setStatus("DRAFT");
        article.setAuthorId(userId);
        article.setViewCount(0);
        article.setLikeCount(0);
        article.setCommentCount(0);
        article.setWordCount(wordCount);
        articleMapper.insert(article);

        ArticleContent content = new ArticleContent();
        content.setArticleId(article.getId());
        content.setContentMd(contentMd);
        content.setContentHtml(contentHtml);
        content.setTocJson(tocJson);
        content.setReadingTime(readingTime);
        contentMapper.insert(content);

        return article.getId();
    }
}
