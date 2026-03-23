package com.blog.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("article_version")
public class ArticleVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long articleId;
    private String title;
    private String contentMd;
    private Integer versionNum;
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
