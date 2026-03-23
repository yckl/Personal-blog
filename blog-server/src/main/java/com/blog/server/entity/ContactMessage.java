package com.blog.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("contact_message")
public class ContactMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String name;
    private String email;
    private String message;
    private String status; // PENDING, REPLIED, ARCHIVED
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    private String replyContent;
    private LocalDateTime repliedAt;
}
