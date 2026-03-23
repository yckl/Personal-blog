package com.blog.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("article_share")
public class ArticleShare {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long articleId;
    private String platform;
    private String ipAddress;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
