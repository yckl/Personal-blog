package com.blog.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("ab_variant")
public class AbVariant {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long experimentId;
    private String name;
    private String configJson;
    private Integer trafficPercent;
    private Integer views;
    private Integer conversions;
}
