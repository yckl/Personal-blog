package com.blog.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("product_purchase")
public class ProductPurchase {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private Long memberId;
    private String orderNo;
    private Integer amountCents;
    private String currency;
    private String status;          // PAID, REFUNDED
    private String downloadToken;
    private Integer downloadCount;
    private Integer maxDownloads;
    private LocalDateTime expiresAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
