package com.blog.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ArticleRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String slug;
    private String excerpt;
    private String coverImage;
    private Long categoryId;
    private List<Long> tagIds;
    private Long seriesId;
    private Integer seriesOrder;
    private String contentMd;
    private String contentHtml;
    private Boolean isTop;
    private Boolean isFeatured;
    private Boolean allowComment;
    private String status;  // DRAFT, PUBLISHED, SCHEDULED, ARCHIVED, PRIVATE, MEMBER_ONLY

    /** Required when status is SCHEDULED */
    private LocalDateTime scheduledAt;
}
