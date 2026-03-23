package com.blog.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("recommend_event")
public class RecommendEvent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long articleId;
    private Long recommendedArticleId;
    private String scene;
    private String eventType;
    private String userIp;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
