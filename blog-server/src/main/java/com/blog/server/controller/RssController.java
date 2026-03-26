package com.blog.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.server.entity.Article;
import com.blog.server.mapper.ArticleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * RSS 2.0 feed endpoint.
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class RssController {

    private final ArticleMapper articleMapper;

    @Value("${app.site-url:http://localhost:3000}")
    private String siteUrl;

    @GetMapping(value = "/feed.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String rssFeed() {
        List<Article> articles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getStatus, "PUBLISHED")
                        .eq(Article::getDeleted, 0)
                        .orderByDesc(Article::getPublishedAt)
                        .last("LIMIT 50"));

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<rss version=\"2.0\" xmlns:atom=\"http://www.w3.org/2005/Atom\">\n<channel>\n");
        sb.append("  <title>My Blog</title>\n");
        sb.append("  <link>").append(siteUrl).append("</link>\n");
        sb.append("  <description>Personal Blog RSS Feed</description>\n");
        sb.append("  <language>zh-cn</language>\n");
        sb.append("  <atom:link href=\"").append(siteUrl).append("/rss.xml\" rel=\"self\" type=\"application/rss+xml\"/>\n");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z");
        for (Article a : articles) {
            sb.append("  <item>\n");
            sb.append("    <title><![CDATA[").append(a.getTitle()).append("]]></title>\n");
            sb.append("    <link>").append(siteUrl).append("/article/").append(a.getSlug()).append("</link>\n");
            sb.append("    <guid>").append(siteUrl).append("/article/").append(a.getSlug()).append("</guid>\n");
            if (a.getExcerpt() != null) {
                sb.append("    <description><![CDATA[").append(a.getExcerpt()).append("]]></description>\n");
            }
            if (a.getPublishedAt() != null) {
                sb.append("    <pubDate>").append(a.getPublishedAt().atZone(java.time.ZoneId.systemDefault()).format(fmt)).append("</pubDate>\n");
            }
            sb.append("  </item>\n");
        }
        sb.append("</channel>\n</rss>");
        return sb.toString();
    }
}
