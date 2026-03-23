package com.blog.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("article")
public class Article {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;
    private String seoTitle;
    private String metaDescription;
    private String canonicalUrl;
    private String ogImage;
    private String slug;
    private String excerpt;
    private String coverImage;
    private Long authorId;
    private Long categoryId;
    private String status;  // DRAFT, PUBLISHED, ARCHIVED
    private String visibility; // PUBLIC, LOGIN_REQUIRED, MEMBER_ONLY, PREMIUM_ONLY
    private Boolean isTop;
    private Boolean isFeatured;
    private Boolean allowComment;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer wordCount;
    private LocalDateTime publishedAt;
    private LocalDateTime scheduledAt;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
