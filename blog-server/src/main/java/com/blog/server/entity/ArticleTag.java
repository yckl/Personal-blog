package com.blog.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("article_tag")
public class ArticleTag {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String slug;
    private String color;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
