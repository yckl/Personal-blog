package com.blog.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.blog.server.common.Result;
import com.blog.server.entity.*;
import com.blog.server.mapper.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * J2: Payment system — order creation, webhook callbacks, refund, order status.
 * Supports Stripe and PayPal (webhook handlers ready for integration).
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentOrderMapper orderMapper;
    private final MemberMapper memberMapper;
    private final MembershipPlanMapper planMapper;

    /**
     * Public: list available payment methods (mock data).
     */
    @GetMapping("/api/payment/methods")
    public Result<List<Map<String, Object>>> getPaymentMethods() {
        List<Map<String, Object>> methods = new ArrayList<>();

        Map<String, Object> stripe = new LinkedHashMap<>();
        stripe.put("id", 1);
        stripe.put("name", "Stripe (信用卡/借记卡)");
        stripe.put("iconUrl", "/files/icons/stripe.svg");
        stripe.put("typeCode", "STRIPE");
        stripe.put("enabled", true);
        methods.add(stripe);

        Map<String, Object> wechat = new LinkedHashMap<>();
        wechat.put("id", 2);
        wechat.put("name", "微信支付");
        wechat.put("iconUrl", "/files/icons/wechat-pay.svg");
        wechat.put("typeCode", "WECHAT_PAY");
        wechat.put("enabled", true);
        methods.add(wechat);

        Map<String, Object> alipay = new LinkedHashMap<>();
        alipay.put("id", 3);
        alipay.put("name", "支付宝");
        alipay.put("iconUrl", "/files/icons/alipay.svg");
        alipay.put("typeCode", "ALIPAY");
        alipay.put("enabled", true);
        methods.add(alipay);

        return Result.ok(methods);
    }

    /**
     * Create a payment order for a membership plan.
     */
    @PostMapping("/api/member/payment/create")
    public Result<Map<String, Object>> createOrder(@RequestBody CreateOrderRequest req) {
        Member member = memberMapper.selectById(req.getMemberId());
        if (member == null) return Result.fail("Member not found");

        MembershipPlan plan = planMapper.selectById(req.getPlanId());
        if (plan == null || !plan.getIsActive()) return Result.fail("Plan not available");

        PaymentOrder order = new PaymentOrder();
        order.setOrderNo(generateOrderNo());
        order.setMemberId(req.getMemberId());
        order.setPlanId(req.getPlanId());
        order.setAmountCents(plan.getPriceCents());
        order.setCurrency(plan.getCurrency());
        order.setPaymentMethod(req.getPaymentMethod());
        order.setPaymentProvider(req.getPaymentProvider() != null ? req.getPaymentProvider() : "stripe");
        order.setStatus("PENDING");
        order.setExpiresAt(LocalDateTime.now().plusHours(24));
        orderMapper.insert(order);

        // In production: call Stripe/PayPal API to create payment session
        // and return the redirect URL or client secret

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderNo", order.getOrderNo());
        result.put("orderId", order.getId());
        result.put("amountCents", order.getAmountCents());
        result.put("currency", order.getCurrency());
        result.put("status", order.getStatus());
        // Placeholder for Stripe checkout URL
        result.put("checkoutUrl", "/checkout/" + order.getOrderNo());
        return Result.ok(result);
    }

    /**
     * Stripe webhook callback.
     * In production: verify Stripe signature before processing.
     */
    @PostMapping("/api/payment/webhook/stripe")
    public Result<Void> stripeWebhook(@RequestBody Map<String, Object> payload) {
        log.info("[Stripe Webhook] Received: {}", payload);

        // Extract event type and order info
        String type = (String) payload.getOrDefault("type", "");
        if ("checkout.session.completed".equals(type) || "payment_intent.succeeded".equals(type)) {
            String orderNo = extractOrderNo(payload);
            if (orderNo != null) {
                completePayment(orderNo, "stripe", String.valueOf(payload.get("id")));
            }
        }
        return Result.ok();
    }

    /**
     * PayPal webhook callback.
     */
    @PostMapping("/api/payment/webhook/paypal")
    public Result<Void> paypalWebhook(@RequestBody Map<String, Object> payload) {
        log.info("[PayPal Webhook] Received: {}", payload);

        String eventType = (String) payload.getOrDefault("event_type", "");
        if ("PAYMENT.CAPTURE.COMPLETED".equals(eventType)) {
            String orderNo = extractOrderNo(payload);
            if (orderNo != null) {
                completePayment(orderNo, "paypal", String.valueOf(payload.get("id")));
            }
        }
        return Result.ok();
    }

    /**
     * Simulate payment completion (for testing).
     */
    @PostMapping("/api/admin/payment/simulate-pay/{orderNo}")
    public Result<Void> simulatePayment(@PathVariable String orderNo) {
        completePayment(orderNo, "simulated", "sim_" + System.currentTimeMillis());
        return Result.ok();
    }

    /**
     * Get order status.
     */
    @GetMapping("/api/member/payment/order/{orderNo}")
    public Result<PaymentOrder> getOrder(@PathVariable String orderNo) {
        PaymentOrder order = orderMapper.selectOne(
                new LambdaQueryWrapper<PaymentOrder>().eq(PaymentOrder::getOrderNo, orderNo));
        return order != null ? Result.ok(order) : Result.fail("Order not found");
    }

    /**
     * List member's orders.
     */
    @GetMapping("/api/member/{memberId}/orders")
    public Result<List<PaymentOrder>> listOrders(@PathVariable Long memberId) {
        return Result.ok(orderMapper.selectList(
                new LambdaQueryWrapper<PaymentOrder>()
                        .eq(PaymentOrder::getMemberId, memberId)
                        .orderByDesc(PaymentOrder::getCreatedAt)));
    }

    /**
     * Admin: initiate refund.
     */
    @PostMapping("/api/admin/payment/refund/{orderNo}")
    public Result<Void> refund(@PathVariable String orderNo) {
        PaymentOrder order = orderMapper.selectOne(
                new LambdaQueryWrapper<PaymentOrder>().eq(PaymentOrder::getOrderNo, orderNo));
        if (order == null) return Result.fail("Order not found");
        if (!"PAID".equals(order.getStatus())) return Result.fail("Only paid orders can be refunded");

        // In production: call Stripe/PayPal refund API
        orderMapper.update(null, new LambdaUpdateWrapper<PaymentOrder>()
                .eq(PaymentOrder::getId, order.getId())
                .set(PaymentOrder::getStatus, "REFUNDED")
                .set(PaymentOrder::getRefundedAt, LocalDateTime.now()));

        // Downgrade member to FREE
        memberMapper.update(null, new LambdaUpdateWrapper<Member>()
                .eq(Member::getId, order.getMemberId())
                .set(Member::getTier, "FREE")
                .set(Member::getTierExpiresAt, null));

        log.info("[Refund] Order {} refunded, member {} downgraded", orderNo, order.getMemberId());
        return Result.ok();
    }

    /**
     * Admin: list all orders.
     */
    @GetMapping("/api/admin/payment/orders")
    public Result<List<PaymentOrder>> adminListOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        var p = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<PaymentOrder>(page, size);
        LambdaQueryWrapper<PaymentOrder> q = new LambdaQueryWrapper<PaymentOrder>()
                .orderByDesc(PaymentOrder::getCreatedAt);
        if (status != null) q.eq(PaymentOrder::getStatus, status);
        orderMapper.selectPage(p, q);
        return Result.ok(p.getRecords());
    }

    // ============ Internal ============

    @org.springframework.transaction.annotation.Transactional
    private void completePayment(String orderNo, String provider, String providerOrderId) {
        PaymentOrder order = orderMapper.selectOne(
                new LambdaQueryWrapper<PaymentOrder>().eq(PaymentOrder::getOrderNo, orderNo));
        if (order == null || !"PENDING".equals(order.getStatus())) return;

        // Update order
        orderMapper.update(null, new LambdaUpdateWrapper<PaymentOrder>()
                .eq(PaymentOrder::getId, order.getId())
                .set(PaymentOrder::getStatus, "PAID")
                .set(PaymentOrder::getPaidAt, LocalDateTime.now())
                .set(PaymentOrder::getPaymentProvider, provider)
                .set(PaymentOrder::getProviderOrderId, providerOrderId));

        // Upgrade member tier
        MembershipPlan plan = planMapper.selectById(order.getPlanId());
        if (plan != null) {
            Member member = memberMapper.selectById(order.getMemberId());
            LocalDateTime currentExpiry = member != null && member.getTierExpiresAt() != null
                    && member.getTierExpiresAt().isAfter(LocalDateTime.now())
                    ? member.getTierExpiresAt() : LocalDateTime.now();

            LocalDateTime newExpiry = currentExpiry.plusMonths(plan.getDurationMonths());

            memberMapper.update(null, new LambdaUpdateWrapper<Member>()
                    .eq(Member::getId, order.getMemberId())
                    .set(Member::getTier, plan.getTier())
                    .set(Member::getTierExpiresAt, newExpiry));

            log.info("[Payment] Member {} upgraded to {} until {}", order.getMemberId(), plan.getTier(), newExpiry);
        }
    }

    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis() + String.format("%04d", new Random().nextInt(10000));
    }

    @SuppressWarnings("unchecked")
    private String extractOrderNo(Map<String, Object> payload) {
        // Try to extract order_no from metadata
        if (payload.containsKey("metadata")) {
            Object metadata = payload.get("metadata");
            if (metadata instanceof Map) {
                return (String) ((Map<String, Object>) metadata).get("order_no");
            }
        }
        return null;
    }

    @Data
    public static class CreateOrderRequest {
        private Long memberId;
        private Long planId;
        private String paymentMethod;   // card, paypal
        private String paymentProvider; // stripe, paypal
    }
}
