package com.blog.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("menu_config")
public class MenuConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String url;
    private String target;      // _self, _blank
    private String icon;
    private Long parentId;
    private Integer sortOrder;
    private Boolean visible;
    private Boolean isExternal;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
