package com.blog.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("member")
public class Member {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String email;
    private String passwordHash;
    private String nickname;
    private String avatar;
    private String tier;            // FREE, PREMIUM
    private LocalDateTime tierExpiresAt;
    private Long currentPlanId;     // tracks the specific plan purchased
    private String status;          // ACTIVE, SUSPENDED, DELETED
    private String loginProvider;   // email, google, github
    private String providerId;
    private LocalDateTime lastLoginAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
