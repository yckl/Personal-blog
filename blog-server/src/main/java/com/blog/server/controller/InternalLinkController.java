package com.blog.server.controller;

import com.blog.server.common.Result;
import com.blog.server.service.InternalLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Internal link suggestion API for editor integration.
 */
@RestController
@RequiredArgsConstructor
public class InternalLinkController {

    private final InternalLinkService internalLinkService;

    /**
     * Get internal link suggestions for an article being edited.
     * Returns articles ranked by relevance with ready-to-insert HTML.
     */
    @GetMapping("/api/admin/articles/{articleId}/internal-links")
    public Result<List<Map<String, Object>>> suggest(
            @PathVariable Long articleId,
            @RequestParam(defaultValue = "10") int limit) {
        return Result.ok(internalLinkService.suggest(articleId, limit));
    }
}
