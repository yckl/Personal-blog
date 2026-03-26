package com.blog.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.server.entity.Article;
import com.blog.server.mapper.ArticleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class SitemapController {

    private final ArticleMapper articleMapper;

    @Value("${app.site-url:http://localhost:3000}")
    private String siteUrl;

    @GetMapping(value = "/sitemap.xml", produces = "application/xml")
    public String getSitemap() {
        List<Article> articles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getStatus, "PUBLISHED")
                        .eq(Article::getDeleted, 0)
                        .orderByDesc(Article::getPublishedAt)
        );

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

        // Add home page
        xml.append("  <url>\n");
        xml.append("    <loc>").append(siteUrl).append("</loc>\n");
        xml.append("    <changefreq>daily</changefreq>\n");
        xml.append("    <priority>1.0</priority>\n");
        xml.append("  </url>\n");

        for (Article article : articles) {
            String pubDate = article.getPublishedAt() != null ?
                    article.getPublishedAt().atZone(java.time.ZoneId.systemDefault()).format(formatter) : "";

            xml.append("  <url>\n");
            xml.append("    <loc>").append(siteUrl).append("/article/").append(article.getSlug()).append("</loc>\n");
            if (!pubDate.isEmpty()) {
                xml.append("    <lastmod>").append(pubDate).append("</lastmod>\n");
            }
            xml.append("    <changefreq>weekly</changefreq>\n");
            xml.append("    <priority>0.8</priority>\n");
            xml.append("  </url>\n");
        }

        xml.append("</urlset>");
        return xml.toString();
    }
}
