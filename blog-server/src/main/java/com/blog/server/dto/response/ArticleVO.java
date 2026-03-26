package com.blog.server.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

@Data
public class ArticleVO {

    private Long id;
    private String title;
    private String slug;
    private String excerpt;
    private String coverImage;
    private String status;
    private Boolean isTop;
    private Boolean isFeatured;
    private Boolean allowComment;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer wordCount;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime publishedAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;

    // Relations
    private String authorName;
    private String authorAvatar;
    private String categoryName;
    private Long categoryId;
    private List<TagVO> tags;

    // Content (only in detail view)
    private String contentMd;
    private String contentHtml;
    private String tocJson;
    private Integer readingTime;

    @Data
    public static class TagVO {
        private Long id;
        private String name;
        private String slug;
        private String color;
    }
}
