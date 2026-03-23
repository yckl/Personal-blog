package com.blog.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("search_log")
public class SearchLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String keyword;
    private Integer resultCount;
    @TableField("ip_address")
    private String userIp;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
