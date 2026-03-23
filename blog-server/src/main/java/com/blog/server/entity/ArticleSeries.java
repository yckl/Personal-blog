package com.blog.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("article_series")
public class ArticleSeries {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String slug;
    private String description;
    private String coverImage;
    private Integer sortOrder;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
