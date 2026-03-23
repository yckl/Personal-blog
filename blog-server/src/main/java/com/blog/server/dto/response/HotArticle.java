package com.blog.server.dto.response;

import lombok.Data;

@Data
public class HotArticle {

    private Long id;
    private String title;
    private String slug;
    private String coverImage;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private String status;
}
