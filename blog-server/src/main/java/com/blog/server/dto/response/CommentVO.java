package com.blog.server.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentVO {

    private Long id;
    private Long articleId;
    private Long parentId;
    private Long rootId;
    private String replyToUserName;
    private String authorName;
    private String authorEmail;
    private String authorUrl;
    private String authorAvatar;
    private String content;
    private String status;
    private Integer likeCount;
    private Boolean isPinned;
    private LocalDateTime createdAt;

    // Nested replies (flattened to 2 levels: root + all descendants)
    private List<CommentVO> children;
}
