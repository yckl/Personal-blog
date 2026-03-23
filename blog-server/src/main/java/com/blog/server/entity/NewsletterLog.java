package com.blog.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("newsletter_log")
public class NewsletterLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long subscriberId;
    private String status;       // PENDING, SENT, FAILED
    private String errorMsg;
    private Boolean opened;
    private LocalDateTime openedAt;
    private Boolean clicked;
    private LocalDateTime clickedAt;
    private LocalDateTime sentAt;
}
