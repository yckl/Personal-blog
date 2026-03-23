package com.blog.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentRequest {

    @NotNull(message = "Article ID is required")
    private Long articleId;

    private Long parentId;

    @NotBlank(message = "Author name is required")
    private String authorName;

    private String authorEmail;
    private String authorUrl;

    @NotBlank(message = "Content is required")
    private String content;
}
