package com.blog.server.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RecentComment {

    private Long id;
    private Long articleId;
    private String articleTitle;
    private String authorName;
    private String authorAvatar;
    private String content;
    private String status;
    private LocalDateTime createdAt;
}
