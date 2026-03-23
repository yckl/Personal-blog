package com.blog.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("payment_order")
public class PaymentOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long memberId;
    private Long planId;
    private Integer amountCents;
    private String currency;
    private String paymentMethod;     // card, paypal, alipay, wechat
    private String paymentProvider;   // stripe, paypal
    private String providerOrderId;
    private String status;            // PENDING, PAID, FAILED, REFUNDED, CANCELLED
    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;
    private LocalDateTime expiresAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
