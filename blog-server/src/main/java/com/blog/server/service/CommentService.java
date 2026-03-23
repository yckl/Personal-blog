package com.blog.server.service;

import com.blog.server.common.PageResult;
import com.blog.server.dto.request.CommentRequest;
import com.blog.server.dto.response.CommentVO;

import java.util.List;

public interface CommentService {

    Long createComment(CommentRequest request, String ipAddress, String userAgent);

    List<CommentVO> getCommentsByArticleId(Long articleId);

    PageResult<CommentVO> listAllComments(Integer page, Integer size, String status, Long articleId);

    void approveComment(Long id);

    void rejectComment(Long id);

    void deleteComment(Long id);

    void likeComment(Long id);

    void togglePinComment(Long id);
}
