package com.blog.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("subscriber")
public class Subscriber {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String email;
    private String name;
    private String status;       // PENDING, CONFIRMED, UNSUBSCRIBED
    private String confirmToken;
    private String source;       // website, article, footer, popup
    private String tags;         // comma-separated preference tags
    private LocalDateTime confirmedAt;
    private LocalDateTime subscribedAt;
    private LocalDateTime unsubscribedAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
