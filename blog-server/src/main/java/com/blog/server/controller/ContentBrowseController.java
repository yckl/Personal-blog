package com.blog.server.controller;

import com.blog.server.common.Result;
import com.blog.server.service.ContentOrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Public APIs for content browsing — categories, tags, series, archive.
 * No authentication required.
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class ContentBrowseController {

    private final ContentOrganizationService orgService;

    // ============ Categories ============

    @GetMapping("/categories/tree")
    public Result<List<Map<String, Object>>> getCategoryTree() {
        return Result.ok(orgService.getCategoryTree());
    }

    @GetMapping("/categories/{slug}/articles")
    public Result<List<Map<String, Object>>> getArticlesByCategory(@PathVariable String slug) {
        return Result.ok(orgService.getArticlesByCategory(slug));
    }

    // ============ Tags ============

    @GetMapping("/tags/all")
    public Result<List<Map<String, Object>>> getTags() {
        return Result.ok(orgService.getTagsWithCount());
    }

    @GetMapping("/tags/{slug}/articles")
    public Result<List<Map<String, Object>>> getArticlesByTag(@PathVariable String slug) {
        return Result.ok(orgService.getArticlesByTag(slug));
    }

    // ============ Series ============

    @GetMapping("/series")
    public Result<List<Map<String, Object>>> getSeries() {
        return Result.ok(orgService.getSeriesWithCount());
    }

    @GetMapping("/series/{slug}")
    public Result<Map<String, Object>> getSeriesDetail(@PathVariable String slug) {
        return Result.ok(orgService.getSeriesDetail(slug));
    }

    // ============ Archive ============

    @GetMapping("/archive")
    public Result<List<Map<String, Object>>> getArchive() {
        return Result.ok(orgService.getArchive());
    }

    @GetMapping("/archive/{year}")
    public Result<List<Map<String, Object>>> getArchiveByYear(@PathVariable int year) {
        return Result.ok(orgService.getArchiveArticles(year, null));
    }

    @GetMapping("/archive/{year}/{month}")
    public Result<List<Map<String, Object>>> getArchiveByMonth(@PathVariable int year, @PathVariable int month) {
        return Result.ok(orgService.getArchiveArticles(year, month));
    }
}
