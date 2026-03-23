package com.blog.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("article_author")
public class ArticleAuthor {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long articleId;
    private Long userId;
    private String role;     // PRIMARY, CO_AUTHOR, REVIEWER
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
