package com.blog.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("welcome_sequence_template")
public class WelcomeSequenceTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer stepOrder;
    private Integer delayDays;
    private String subject;
    private String contentHtml;
    private Boolean isActive;
    private String recommendArticleIds;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
