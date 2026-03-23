package com.blog.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_task_log")
public class AiTaskLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long articleId;
    private String taskType;
    private String inputText;
    private String resultJson;
    private String model;
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
