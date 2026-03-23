package com.blog.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_role")
public class SysRole {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String roleName;
    private String roleKey;
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
