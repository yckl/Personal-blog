package com.blog.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_article_history")
public class ArticleHistory {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long articleId;
    private String title;
    private String contentMd;
    private String versionHash;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
