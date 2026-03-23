package com.blog.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * I3: Audit log for tracking admin operations.
 */
@Data
@TableName("audit_log")
public class AuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String action;       // CREATE, UPDATE, DELETE, LOGIN, PUBLISH, etc.
    private String resource;     // article, comment, subscriber, setting, etc.
    private Long resourceId;
    private String detail;       // JSON detail of the change
    private String ipAddress;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
