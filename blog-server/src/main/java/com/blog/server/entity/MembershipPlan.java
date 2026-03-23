package com.blog.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("membership_plan")
public class MembershipPlan {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String tier;            // FREE, PREMIUM
    private Integer durationMonths;
    private Integer priceCents;
    private String currency;
    private String features;        // comma-separated or JSON
    private Boolean isActive;
    private Integer sortOrder;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
