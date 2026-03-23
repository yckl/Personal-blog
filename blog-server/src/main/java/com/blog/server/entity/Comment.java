package com.blog.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("comment")
public class Comment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long articleId;
    private Long parentId;
    private Long rootId;
    private String authorName;
    private String authorEmail;
    private String authorUrl;
    private String authorAvatar;
    private String content;
    private String status;  // PENDING, APPROVED, REJECTED
    private String ipAddress;
    private String userAgent;
    private Integer likeCount;
    private Boolean isPinned;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
