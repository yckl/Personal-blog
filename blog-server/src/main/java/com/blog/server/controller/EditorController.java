package com.blog.server.controller;

import com.blog.server.common.Result;
import com.blog.server.entity.SysUser;
import com.blog.server.service.EditorService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/editor")
@RequiredArgsConstructor
public class EditorController {

    private final EditorService editorService;

    /** Generate table of contents from markdown */
    @PostMapping("/toc")
    public Result<List<Map<String, Object>>> generateToc(@RequestBody ContentBody body) {
        return Result.ok(editorService.generateToc(body.getContentMd()));
    }

    /** Get reading time and word count for content */
    @PostMapping("/stats")
    public Result<Map<String, Object>> getStats(@RequestBody ContentBody body) {
        int readingTime = editorService.estimateReadingTime(body.getContentMd());
        int wordCount = editorService.countWords(body.getContentMd());
        return Result.ok(Map.of("readingTime", readingTime, "wordCount", wordCount));
    }

    /** Auto-save draft */
    @PostMapping("/save-draft")
    public Result<Long> saveDraft(@RequestBody DraftBody body,
                                  @AuthenticationPrincipal SysUser user) {
        Long id = editorService.saveDraft(body.getArticleId(), body.getTitle(),
                body.getContentMd(), body.getContentHtml(), user.getId());
        return Result.ok(id);
    }

    @Data
    public static class ContentBody {
        private String contentMd;
    }

    @Data
    public static class DraftBody {
        private Long articleId;
        private String title;
        private String contentMd;
        private String contentHtml;
    }
}
