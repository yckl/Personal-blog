package com.blog.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private String email;
    private String bio;
    private Integer status;

    private Boolean twoFactorEnabled;
    private String twoFactorSecret;
    private LocalDateTime lastLoginAt;
    private String lastLoginIp;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** Non-persistent: populated by service layer for API responses */
    @TableField(exist = false)
    private List<String> roles;

    @TableField(exist = false)
    private List<String> permissions;
}
