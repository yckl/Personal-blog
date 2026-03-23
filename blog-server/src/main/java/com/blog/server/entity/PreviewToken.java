package com.blog.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("preview_token")
public class PreviewToken {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long articleId;
    private String token;
    private LocalDateTime expiresAt;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
