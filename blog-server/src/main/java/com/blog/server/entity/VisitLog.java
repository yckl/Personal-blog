package com.blog.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("visit_log")
public class VisitLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long articleId;
    private String pageUrl;
    private String visitorId;
    private String pageType;    // article, home, archive, category, tag, about, search
    private String ipAddress;
    private String userAgent;
    private String referer;
    private String country;
    private String city;
    private String deviceType;  // desktop, mobile, tablet
    private String browser;
    private String os;
    private Integer staySeconds;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
