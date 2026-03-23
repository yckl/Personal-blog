package com.blog.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("article_manual_boost")
public class ArticleManualBoost {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long articleId;
    private Integer boostLevel;
    private String targetScene;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
