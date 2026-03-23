package com.blog.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("newsletter_task")
public class NewsletterTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long articleId;
    private String subject;
    private String content;
    private String contentHtml;
    private String audienceType;  // ALL, FREE, PREMIUM, TAG:xxx
    private String status;       // DRAFT, SCHEDULED, SENDING, SENT, FAILED
    private Integer totalCount;
    private Integer sentCount;
    private Integer failedCount;
    private LocalDateTime scheduledAt;
    private Long createdBy;
    private LocalDateTime sentAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
