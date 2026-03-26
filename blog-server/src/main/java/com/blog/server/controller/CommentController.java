package com.blog.server.controller;

import com.blog.server.common.PageResult;
import com.blog.server.common.Result;
import com.blog.server.dto.request.CommentRequest;
import com.blog.server.dto.response.CommentVO;
import com.blog.server.service.CommentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // ============ Public Endpoints ============

    @PostMapping("/api/comments")
    public Result<Long> createComment(@Valid @RequestBody CommentRequest request,
                                      HttpServletRequest httpRequest) {
        String ip = com.blog.server.common.IpUtils.getClientIp(httpRequest);
        String ua = httpRequest.getHeader("User-Agent");
        return Result.ok(commentService.createComment(request, ip, ua));
    }

    @GetMapping("/api/comments/article/{articleId}")
    public Result<List<CommentVO>> getCommentsByArticle(@PathVariable Long articleId) {
        return Result.ok(commentService.getCommentsByArticleId(articleId));
    }

    @PostMapping("/api/comments/{id}/like")
    public Result<Void> likeComment(@PathVariable Long id) {
        commentService.likeComment(id);
        return Result.ok();
    }

    @PostMapping("/api/comments/{id}/unlike")
    public Result<Void> unlikeComment(@PathVariable Long id) {
        commentService.unlikeComment(id);
        return Result.ok();
    }

    // ============ Admin Endpoints ============

    @GetMapping("/api/admin/comments")
    public Result<PageResult<CommentVO>> listAllComments(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long articleId) {
        return Result.ok(commentService.listAllComments(page, size, status, articleId));
    }

    @PutMapping("/api/admin/comments/{id}/approve")
    public Result<Void> approveComment(@PathVariable Long id) {
        commentService.approveComment(id);
        return Result.ok();
    }

    @PutMapping("/api/admin/comments/{id}/reject")
    public Result<Void> rejectComment(@PathVariable Long id) {
        commentService.rejectComment(id);
        return Result.ok();
    }

    @PutMapping("/api/admin/comments/{id}/pin")
    public Result<Void> togglePinComment(@PathVariable Long id) {
        commentService.togglePinComment(id);
        return Result.ok();
    }

    @DeleteMapping("/api/admin/comments/{id}")
    public Result<Void> deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id);
        return Result.ok();
    }
}
