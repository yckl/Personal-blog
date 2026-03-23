package com.blog.server.controller;

import com.blog.server.common.Result;
import com.blog.server.entity.ArticleVersion;
import com.blog.server.service.ExportImportService;
import com.blog.server.service.VersionService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class BackupController {

    private final VersionService versionService;
    private final ExportImportService exportImportService;

    // ============ Article Versions ============

    @GetMapping("/api/admin/article/{id}/versions")
    public Result<List<ArticleVersion>> listVersions(@PathVariable Long id) {
        return Result.ok(versionService.listVersions(id));
    }

    @GetMapping("/api/admin/article/version/{versionId}")
    public Result<ArticleVersion> getVersion(@PathVariable Long versionId) {
        return Result.ok(versionService.getVersion(versionId));
    }

    @PostMapping("/api/admin/article/{id}/rollback/{versionId}")
    public Result<Void> rollback(@PathVariable Long id, @PathVariable Long versionId) {
        versionService.rollbackToVersion(id, versionId);
        return Result.ok();
    }

    // ============ Export ============

    @GetMapping("/api/admin/export/article/{id}")
    @SuppressWarnings("null")
    public ResponseEntity<byte[]> exportArticle(@PathVariable Long id) {
        byte[] md = exportImportService.exportArticleMarkdown(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=article-" + id + ".md")
                .contentType(MediaType.TEXT_PLAIN)
                .body(md);
    }

    @GetMapping("/api/admin/export/site")
    public void exportSiteZip(HttpServletResponse response) {
        exportImportService.exportSiteMarkdownZip(response);
    }

    @GetMapping("/api/admin/export/site-json")
    @SuppressWarnings("null")
    public ResponseEntity<byte[]> exportSiteJson() {
        byte[] json = exportImportService.exportSiteJson();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=blog-export.json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
    }

    // ============ Import ============

    @PostMapping("/api/admin/import/site")
    public Result<Void> importSiteJson(@RequestParam("file") MultipartFile file) {
        exportImportService.importSiteJson(file);
        return Result.ok();
    }

    @PostMapping("/api/admin/import/markdown")
    public Result<Void> importMarkdown(@RequestParam("file") MultipartFile file) {
        exportImportService.importMarkdownZip(file);
        return Result.ok();
    }
}
