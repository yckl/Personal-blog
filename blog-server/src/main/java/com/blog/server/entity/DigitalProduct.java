package com.blog.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("digital_product")
public class DigitalProduct {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String slug;
    private String description;
    private String coverImage;
    private String productType;    // EBOOK, COURSE, RESOURCE_PACK
    private Integer priceCents;
    private String currency;
    private String fileUrl;
    private Long fileSizeBytes;
    private String previewUrl;
    private String status;         // DRAFT, PUBLISHED, ARCHIVED
    private String visibility;     // PUBLIC, MEMBER_ONLY, PREMIUM_ONLY
    private Integer downloadCount;
    private Integer sortOrder;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
