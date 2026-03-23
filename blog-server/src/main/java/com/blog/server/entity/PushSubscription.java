package com.blog.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("push_subscription")
public class PushSubscription {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String endpoint;
    private String p256dhKey;
    private String authKey;
    private Long memberId;
    private String visitorId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
