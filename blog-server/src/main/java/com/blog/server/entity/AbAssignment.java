package com.blog.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ab_assignment")
public class AbAssignment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long experimentId;
    private Long variantId;
    private String visitorId;
    private Integer converted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
