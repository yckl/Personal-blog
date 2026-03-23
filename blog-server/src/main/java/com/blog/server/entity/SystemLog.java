package com.blog.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("system_log")
public class SystemLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String operation;
    private String method;
    private String params;
    private String ipAddress;
    private Long duration;
    private Integer status;
    private String errorMsg;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
