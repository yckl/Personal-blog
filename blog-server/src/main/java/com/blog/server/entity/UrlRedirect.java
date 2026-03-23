package com.blog.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("url_redirect")
public class UrlRedirect {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String oldPath;
    private String newPath;
    private Integer statusCode;
    private Boolean isActive;
    private Integer hitCount;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
