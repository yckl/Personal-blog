package com.blog.server.service;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ExportImportService {

    /** Export a single article as Markdown */
    byte[] exportArticleMarkdown(Long articleId);

    /** Export all articles as a ZIP of Markdown files */
    void exportSiteMarkdownZip(HttpServletResponse response);

    /** Export entire site as JSON */
    byte[] exportSiteJson();

    /** Import site data from JSON */
    void importSiteJson(MultipartFile file);

    /** Import articles from a ZIP of Markdown files */
    void importMarkdownZip(MultipartFile file);
}
