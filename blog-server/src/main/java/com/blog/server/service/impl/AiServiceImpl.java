package com.blog.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.server.entity.AiTaskLog;
import com.blog.server.entity.Article;
import com.blog.server.mapper.AiTaskLogMapper;
import com.blog.server.mapper.ArticleMapper;
import com.blog.server.service.AiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final AiTaskLogMapper taskLogMapper;
    private final ArticleMapper articleMapper;
    private final ObjectMapper objectMapper;

    @Value("${ai.api-url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${ai.api-key:}")
    private String apiKey;

    @Value("${ai.model:gpt-3.5-turbo}")
    private String model;

    @Value("${ai.max-input-length:8000}")
    private int maxInputLength;

    private final RestTemplate restTemplate = new RestTemplate();

    // ---- Public API ----

    @Override
    public String generateTitle(String content, Long articleId, Long userId) {
        return callAi("title", articleId, userId, content,
                "You are a professional blog editor. Generate 3 compelling article titles based on the content. Return a JSON array of strings.",
                "Generate 3 article titles for:\n\n" + truncate(content));
    }

    @Override
    public String generateSummary(String content, Long articleId, Long userId) {
        return callAi("summary", articleId, userId, content,
                "You are a blog editor. Write a concise, engaging excerpt/summary (2-3 sentences, max 160 characters) for the article.",
                "Write an excerpt for:\n\n" + truncate(content));
    }

    @Override
    public String generateSeoTitle(String content, String currentTitle, Long articleId, Long userId) {
        String input = "Current title: " + currentTitle + "\n\nContent:\n" + truncate(content);
        return callAi("seo_title", articleId, userId, input,
                "You are an SEO expert. Generate 3 SEO-optimized titles (50-60 chars each). Return a JSON array of strings.",
                "Generate SEO titles for:\n\n" + input);
    }

    @Override
    public String generateMetaDescription(String content, Long articleId, Long userId) {
        return callAi("meta_description", articleId, userId, content,
                "You are an SEO expert. Write a compelling meta description (120-155 characters) for search engines.",
                "Write a meta description for:\n\n" + truncate(content));
    }

    @Override
    public String generateOutline(String topic, Long articleId, Long userId) {
        return callAi("outline", articleId, userId, topic,
                "You are a content strategist. Create a detailed article outline with H2 and H3 headings in Markdown format.",
                "Create an article outline for the topic: " + topic);
    }

    @Override
    public String recommendTags(String content, Long articleId, Long userId) {
        // Get existing tags for context
        return callAi("tags", articleId, userId, content,
                "You are a blog editor. Suggest 5-8 relevant tags for this article. Return a JSON array of strings. Prefer existing common tags over novel ones.",
                "Suggest tags for:\n\n" + truncate(content));
    }

    @Override
    public String generateNewsletter(String content, Long articleId, Long userId) {
        return callAi("newsletter", articleId, userId, content,
                "You are a newsletter writer. Write a brief, engaging newsletter summary (3-4 sentences) that would entice subscribers to read the full article.",
                "Write a newsletter summary for:\n\n" + truncate(content));
    }

    @Override
    public String recommendInternalLinks(String content, Long articleId, Long userId) {
        // Fetch existing article titles for linking
        List<Article> articles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>().select(Article::getId, Article::getTitle, Article::getSlug));
        StringBuilder existing = new StringBuilder("Existing articles:\n");
        for (Article a : articles) {
            if (articleId != null && a.getId().equals(articleId)) continue;
            existing.append("- ").append(a.getTitle()).append(" (slug: ").append(a.getSlug()).append(")\n");
        }

        String input = existing + "\n\nCurrent article:\n" + truncate(content);
        return callAi("internal_links", articleId, userId, input,
                "You are a content strategist. Suggest 3-5 internal links from the existing articles list that are relevant to the current article. Return a JSON array of objects with 'title', 'slug', and 'reason' fields.",
                input);
    }

    // ---- Core LLM call ----

    @SuppressWarnings("null")
    private String callAi(String taskType, Long articleId, Long userId, String inputText, String systemPrompt, String userPrompt) {
        // If no API key, return a helpful placeholder
        if (apiKey == null || apiKey.isEmpty()) {
            String placeholder = getFallbackResult(taskType, inputText);
            saveLog(taskType, articleId, userId, inputText, placeholder);
            return placeholder;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
            ));
            body.put("temperature", 0.7);
            body.put("max_tokens", 1000);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, request, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            String result = root.path("choices").get(0).path("message").path("content").asText();

            saveLog(taskType, articleId, userId, truncate(inputText), result);
            return result;
        } catch (Exception e) {
            log.error("AI API call failed for task {}: {}", taskType, e.getMessage());
            String fallback = getFallbackResult(taskType, inputText);
            saveLog(taskType, articleId, userId, truncate(inputText), "ERROR: " + e.getMessage());
            return fallback;
        }
    }

    private void saveLog(String taskType, Long articleId, Long userId, String input, String result) {
        AiTaskLog aiLog = new AiTaskLog();
        aiLog.setTaskType(taskType);
        aiLog.setArticleId(articleId);
        aiLog.setCreatedBy(userId);
        aiLog.setInputText(input != null && input.length() > 2000 ? input.substring(0, 2000) : input);
        aiLog.setResultJson(result);
        aiLog.setModel(model);
        taskLogMapper.insert(aiLog);
    }

    private String truncate(String text) {
        if (text == null) return "";
        return text.length() > maxInputLength ? text.substring(0, maxInputLength) : text;
    }

    /** Provide useful mock results when no API key is configured */
    private String getFallbackResult(String taskType, String input) {
        String snippet = input != null && input.length() > 30 ? input.substring(0, 30) : (input != null ? input : "");
        return switch (taskType) {
            case "title" -> "[\"" + snippet + " — 深度解析与实战指南\", \"从零理解" + snippet + "：核心概念全解\", \"" + snippet + "最佳实践总结\"]";
            case "summary" -> "本文深入探讨了" + snippet + "的核心概念与实践应用，为读者提供了清晰的理解路径和实用的操作建议。（AI 未配置，此为默认摘要，请在 application.yml 中设置 ai.api-key）";
            case "seo_title" -> "[\"" + snippet + " 完全指南 | 博客\", \"深入理解" + snippet + " - 实战教程\", \"" + snippet + " 从入门到精通\"]";
            case "meta_description" -> "深入了解" + snippet + "的核心概念、最佳实践与常见问题。本文为您提供全面的指导。（请配置 AI API Key 获取真实结果）";
            case "outline" -> "## 引言\n\n## 核心概念\n\n### 概念一\n\n### 概念二\n\n## 实践应用\n\n### 场景分析\n\n### 代码示例\n\n## 常见问题\n\n## 总结与展望";
            case "tags" -> "[\"技术\", \"教程\", \"最佳实践\", \"开发\", \"指南\"]";
            case "newsletter" -> "本期推荐文章深入探讨了" + snippet + "的关键要点。无论你是初学者还是有经验的开发者，都能从中获得启发。点击阅读全文了解更多。";
            case "internal_links" -> "[]";
            default -> "AI 未配置。请在 application.yml 中设置 ai.api-key 以获取真实结果。";
        };
    }
}
