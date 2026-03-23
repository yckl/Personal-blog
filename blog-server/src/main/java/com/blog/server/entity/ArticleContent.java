package com.blog.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("article_content")
public class ArticleContent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long articleId;
    private String contentMd;
    private String contentHtml;
    private String tocJson;
    private Integer readingTime;
}
