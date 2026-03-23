package com.blog.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("experience_timeline")
public class ExperienceTimeline {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String year;
    private String title;
    private String description;
    private Integer sortOrder;
    private Boolean isVisible;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
