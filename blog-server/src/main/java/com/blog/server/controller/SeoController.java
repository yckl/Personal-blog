package com.blog.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.blog.server.common.Result;
import com.blog.server.entity.*;
import com.blog.server.mapper.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SEO Controller — sitemap.xml, robots.txt, 301 redirects, site SEO config.
 */
@RestController
@RequiredArgsConstructor
public class SeoController {

    private final ArticleMapper articleMapper;
    private final UrlRedirectMapper redirectMapper;
    private final SiteSeoConfigMapper configMapper;

    // ============ Public SEO Endpoints ============

    /**
     * Generate sitemap.xml dynamically from published articles.
     */
    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @SuppressWarnings("null")
    public ResponseEntity<String> sitemap() {
        List<SiteSeoConfig> configs = configMapper.selectList(null);
        String siteUrl = getConfigValue(configs, "site_url", "http://localhost:5173");

        List<Article> articles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getStatus, "PUBLISHED")
                        .orderByDesc(Article::getPublishedAt));

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // Homepage
        xml.append("  <url>\n");
        xml.append("    <loc>").append(siteUrl).append("/</loc>\n");
        xml.append("    <changefreq>daily</changefreq>\n");
        xml.append("    <priority>1.0</priority>\n");
        xml.append("  </url>\n");

        // Static pages
        for (String page : List.of("/articles", "/archive", "/about", "/contact")) {
            xml.append("  <url>\n");
            xml.append("    <loc>").append(siteUrl).append(page).append("</loc>\n");
            xml.append("    <changefreq>weekly</changefreq>\n");
            xml.append("    <priority>0.7</priority>\n");
            xml.append("  </url>\n");
        }

        // Articles
        for (Article a : articles) {
            xml.append("  <url>\n");
            xml.append("    <loc>").append(siteUrl).append("/article/").append(a.getSlug()).append("</loc>\n");
            if (a.getUpdatedAt() != null) {
                xml.append("    <lastmod>").append(a.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE)).append("</lastmod>\n");
            } else if (a.getPublishedAt() != null) {
                xml.append("    <lastmod>").append(a.getPublishedAt().format(DateTimeFormatter.ISO_LOCAL_DATE)).append("</lastmod>\n");
            }
            xml.append("    <changefreq>monthly</changefreq>\n");
            xml.append("    <priority>0.8</priority>\n");
            xml.append("  </url>\n");
        }

        xml.append("</urlset>");
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).body(xml.toString());
    }

    /**
     * Generate robots.txt dynamically.
     */
    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String robotsTxt() {
        List<SiteSeoConfig> configs = configMapper.selectList(null);
        String siteUrl = getConfigValue(configs, "site_url", "http://localhost:5173");
        String customRules = getConfigValue(configs, "robots_custom_rules", "");

        StringBuilder sb = new StringBuilder();
        sb.append("User-agent: *\n");
        sb.append("Allow: /\n");
        sb.append("Disallow: /api/\n");
        sb.append("Disallow: /admin/\n");
        sb.append("\n");
        if (!customRules.isEmpty()) {
            sb.append(customRules).append("\n\n");
        }
        sb.append("Sitemap: ").append(siteUrl).append("/sitemap.xml\n");
        return sb.toString();
    }

    /**
     * Handle 301 redirects — called by a frontend or reverse proxy.
     */
    @GetMapping("/api/redirect")
    public Result<Map<String, Object>> checkRedirect(@RequestParam String path) {
        UrlRedirect redirect = redirectMapper.selectOne(
                new LambdaQueryWrapper<UrlRedirect>()
                        .eq(UrlRedirect::getOldPath, path)
                        .eq(UrlRedirect::getIsActive, true));
        if (redirect == null) {
            return Result.ok(Map.of("found", false));
        }
        // Increment hit count
        redirectMapper.update(null, new LambdaUpdateWrapper<UrlRedirect>()
                .eq(UrlRedirect::getId, redirect.getId())
                .setSql("hit_count = hit_count + 1"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("found", true);
        result.put("newPath", redirect.getNewPath());
        result.put("statusCode", redirect.getStatusCode());
        return Result.ok(result);
    }

    /**
     * Get site-wide SEO configuration (for frontend meta rendering).
     */
    @GetMapping("/api/site/seo")
    public Result<Map<String, String>> getSeoConfig() {
        List<SiteSeoConfig> configs = configMapper.selectList(null);
        Map<String, String> map = configs.stream()
                .collect(Collectors.toMap(SiteSeoConfig::getConfigKey,
                        c -> c.getConfigValue() != null ? c.getConfigValue() : "",
                        (a, b) -> a));
        return Result.ok(map);
    }

    // ============ Admin SEO Endpoints ============

    /**
     * Update site SEO config.
     */
    @PutMapping("/api/admin/seo/config")
    public Result<Void> updateSeoConfig(@RequestBody Map<String, String> configs) {
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            SiteSeoConfig existing = configMapper.selectOne(
                    new LambdaQueryWrapper<SiteSeoConfig>()
                            .eq(SiteSeoConfig::getConfigKey, entry.getKey()));
            if (existing != null) {
                existing.setConfigValue(entry.getValue());
                configMapper.updateById(existing);
            } else {
                SiteSeoConfig config = new SiteSeoConfig();
                config.setConfigKey(entry.getKey());
                config.setConfigValue(entry.getValue());
                configMapper.insert(config);
            }
        }
        return Result.ok();
    }

    /**
     * Create a 301 redirect.
     */
    @PostMapping("/api/admin/redirect")
    public Result<UrlRedirect> createRedirect(@RequestBody RedirectRequest request) {
        UrlRedirect redirect = new UrlRedirect();
        redirect.setOldPath(request.getOldPath());
        redirect.setNewPath(request.getNewPath());
        redirect.setStatusCode(request.getStatusCode() != null ? request.getStatusCode() : 301);
        redirect.setIsActive(true);
        redirect.setHitCount(0);
        redirectMapper.insert(redirect);
        return Result.ok(redirect);
    }

    /**
     * List all redirects.
     */
    @GetMapping("/api/admin/redirects")
    public Result<List<UrlRedirect>> listRedirects() {
        return Result.ok(redirectMapper.selectList(
                new LambdaQueryWrapper<UrlRedirect>().orderByDesc(UrlRedirect::getCreatedAt)));
    }

    /**
     * Delete a redirect.
     */
    @DeleteMapping("/api/admin/redirect/{id}")
    public Result<Void> deleteRedirect(@PathVariable Long id) {
        redirectMapper.deleteById(id);
        return Result.ok();
    }

    // ============ Helpers ============

    private String getConfigValue(List<SiteSeoConfig> configs, String key, String defaultValue) {
        return configs.stream()
                .filter(c -> key.equals(c.getConfigKey()))
                .map(c -> c.getConfigValue() != null ? c.getConfigValue() : defaultValue)
                .findFirst().orElse(defaultValue);
    }

    @Data
    public static class RedirectRequest {
        private String oldPath;
        private String newPath;
        private Integer statusCode;
    }
}
