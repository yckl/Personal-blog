package com.blog.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ab_experiment")
public class AbExperiment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private String status;       // DRAFT, RUNNING, ENDED
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
