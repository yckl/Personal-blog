package com.blog.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("welcome_sequence_log")
public class WelcomeSequenceLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long subscriberId;
    private Long templateId;
    private Integer stepOrder;
    private String status;       // PENDING, SENT, FAILED, SKIPPED
    private LocalDateTime scheduledAt;
    private LocalDateTime sentAt;
    private String errorMsg;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
