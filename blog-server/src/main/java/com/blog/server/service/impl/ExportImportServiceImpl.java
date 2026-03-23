package com.blog.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.server.entity.*;
import com.blog.server.exception.BusinessException;
import com.blog.server.mapper.*;
import com.blog.server.service.ExportImportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportImportServiceImpl implements ExportImportService {

    private final ArticleMapper articleMapper;
    private final ArticleContentMapper contentMapper;
    private final ArticleCategoryMapper categoryMapper;
    private final ArticleTagMapper tagMapper;
    private final ArticleTagRelMapper tagRelMapper;
    private final SiteConfigMapper siteConfigMapper;
    private final MenuConfigMapper menuConfigMapper;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);

    @Override
    public byte[] exportArticleMarkdown(Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) throw new BusinessException(404, "Article not found");

        ArticleContent content = contentMapper.selectOne(
                new LambdaQueryWrapper<ArticleContent>().eq(ArticleContent::getArticleId, articleId));

        StringBuilder sb = new StringBuilder();
        // YAML front matter
        sb.append("---\n");
        sb.append("title: \"").append(escape(article.getTitle())).append("\"\n");
        sb.append("slug: \"").append(article.getSlug()).append("\"\n");
        sb.append("status: ").append(article.getStatus()).append("\n");
        if (article.getExcerpt() != null) sb.append("excerpt: \"").append(escape(article.getExcerpt())).append("\"\n");
        if (article.getCoverImage() != null) sb.append("coverImage: \"").append(article.getCoverImage()).append("\"\n");
        if (article.getPublishedAt() != null) sb.append("publishedAt: ").append(article.getPublishedAt()).append("\n");
        sb.append("createdAt: ").append(article.getCreatedAt()).append("\n");
        sb.append("---\n\n");

        if (content != null && content.getContentMd() != null) {
            sb.append(content.getContentMd());
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void exportSiteMarkdownZip(HttpServletResponse response) {
        List<Article> articles = articleMapper.selectList(new LambdaQueryWrapper<>());
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=blog-export.zip");

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            for (Article article : articles) {
                byte[] md = exportArticleMarkdown(article.getId());
                String filename = sanitizeFilename(article.getTitle()) + ".md";
                zos.putNextEntry(new ZipEntry(filename));
                zos.write(md);
                zos.closeEntry();
            }
        } catch (IOException e) {
            log.error("Failed to export ZIP", e);
            throw new BusinessException("Export failed");
        }
    }

    @Override
    public byte[] exportSiteJson() {
        try {
            SiteExportData data = new SiteExportData();
            data.setExportedAt(LocalDateTime.now());

            // Articles + content
            List<Article> articles = articleMapper.selectList(new LambdaQueryWrapper<>());
            List<SiteExportData.ArticleData> articleDataList = new ArrayList<>();
            for (Article a : articles) {
                SiteExportData.ArticleData ad = new SiteExportData.ArticleData();
                ad.setArticle(a);
                ArticleContent c = contentMapper.selectOne(
                        new LambdaQueryWrapper<ArticleContent>().eq(ArticleContent::getArticleId, a.getId()));
                ad.setContent(c);
                List<ArticleTagRel> rels = tagRelMapper.selectList(
                        new LambdaQueryWrapper<ArticleTagRel>().eq(ArticleTagRel::getArticleId, a.getId()));
                ad.setTagRelations(rels);
                articleDataList.add(ad);
            }
            data.setArticles(articleDataList);

            // Categories
            data.setCategories(categoryMapper.selectList(new LambdaQueryWrapper<>()));
            // Tags
            data.setTags(tagMapper.selectList(new LambdaQueryWrapper<>()));
            // Site config
            data.setSiteConfigs(siteConfigMapper.selectList(new LambdaQueryWrapper<>()));
            // Menus
            data.setMenus(menuConfigMapper.selectList(new LambdaQueryWrapper<>()));

            return objectMapper.writeValueAsBytes(data);
        } catch (Exception e) {
            log.error("Failed to export JSON", e);
            throw new BusinessException("Export failed");
        }
    }

    @Override
    @Transactional
    public void importSiteJson(MultipartFile file) {
        try {
            SiteExportData data = objectMapper.readValue(file.getInputStream(), SiteExportData.class);

            // Import categories
            if (data.getCategories() != null) {
                for (ArticleCategory cat : data.getCategories()) {
                    cat.setId(null);
                    categoryMapper.insert(cat);
                }
            }
            // Import tags
            if (data.getTags() != null) {
                for (ArticleTag tag : data.getTags()) {
                    tag.setId(null);
                    tagMapper.insert(tag);
                }
            }
            // Import articles
            if (data.getArticles() != null) {
                for (SiteExportData.ArticleData ad : data.getArticles()) {
                    Article article = ad.getArticle();
                    article.setId(null);
                    articleMapper.insert(article);

                    if (ad.getContent() != null) {
                        ArticleContent c = ad.getContent();
                        c.setId(null);
                        c.setArticleId(article.getId());
                        contentMapper.insert(c);
                    }
                    if (ad.getTagRelations() != null) {
                        for (ArticleTagRel rel : ad.getTagRelations()) {
                            rel.setId(null);
                            rel.setArticleId(article.getId());
                            tagRelMapper.insert(rel);
                        }
                    }
                }
            }
            // Import site config
            if (data.getSiteConfigs() != null) {
                for (SiteConfig sc : data.getSiteConfigs()) {
                    SiteConfig existing = siteConfigMapper.selectOne(
                            new LambdaQueryWrapper<SiteConfig>().eq(SiteConfig::getConfigKey, sc.getConfigKey()));
                    if (existing != null) {
                        existing.setConfigValue(sc.getConfigValue());
                        siteConfigMapper.updateById(existing);
                    } else {
                        sc.setId(null);
                        siteConfigMapper.insert(sc);
                    }
                }
            }
            // Import menus
            if (data.getMenus() != null) {
                for (MenuConfig m : data.getMenus()) {
                    m.setId(null);
                    menuConfigMapper.insert(m);
                }
            }
        } catch (IOException e) {
            log.error("Failed to import JSON", e);
            throw new BusinessException("Import failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void importMarkdownZip(MultipartFile file) {
        try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String name = entry.getName();
                if (!name.endsWith(".md")) continue;

                String content = new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                String title = name.replace(".md", "");
                String mdBody = content;

                // Parse front matter if present
                if (content.startsWith("---")) {
                    int endIdx = content.indexOf("---", 3);
                    if (endIdx > 0) {
                        String frontMatter = content.substring(3, endIdx).trim();
                        mdBody = content.substring(endIdx + 3).trim();
                        // Extract title from front matter
                        for (String line : frontMatter.split("\n")) {
                            if (line.startsWith("title:")) {
                                title = line.substring(6).trim().replaceAll("^\"|\"$", "");
                            }
                        }
                    }
                }

                Article article = new Article();
                article.setTitle(title);
                article.setSlug(title.toLowerCase().replaceAll("[^a-z0-9]+", "-") + "-" + System.currentTimeMillis() % 10000);
                article.setStatus("DRAFT");
                article.setViewCount(0);
                article.setLikeCount(0);
                article.setCommentCount(0);
                articleMapper.insert(article);

                ArticleContent ac = new ArticleContent();
                ac.setArticleId(article.getId());
                ac.setContentMd(mdBody);
                contentMapper.insert(ac);

                zis.closeEntry();
            }
        } catch (IOException e) {
            log.error("Failed to import Markdown ZIP", e);
            throw new BusinessException("Import failed: " + e.getMessage());
        }
    }

    // ---- Helpers ----

    private String escape(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }

    private String sanitizeFilename(String name) {
        return name == null ? "untitled" : name.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5._-]", "_").substring(0, Math.min(name.length(), 100));
    }

    // ---- Export Data Structure ----

    @Data
    public static class SiteExportData {
        private LocalDateTime exportedAt;
        private List<ArticleData> articles;
        private List<ArticleCategory> categories;
        private List<ArticleTag> tags;
        private List<SiteConfig> siteConfigs;
        private List<MenuConfig> menus;

        @Data
        public static class ArticleData {
            private Article article;
            private ArticleContent content;
            private List<ArticleTagRel> tagRelations;
        }
    }
}
