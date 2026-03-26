package com.blog.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.blog.server.common.Result;
import com.blog.server.entity.*;
import com.blog.server.mapper.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * J3: Digital product sales — eBooks, courses, resource packs.
 */
@RestController
@RequiredArgsConstructor
public class DigitalProductController {

    private final DigitalProductMapper productMapper;
    private final ProductPurchaseMapper purchaseMapper;
    private final MemberMapper memberMapper;

    // ============ Public ============

    /**
     * List published products (public store).
     */
    @GetMapping("/api/products")
    public Result<List<DigitalProduct>> listProducts(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        var p = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<DigitalProduct>(page, size);
        LambdaQueryWrapper<DigitalProduct> q = new LambdaQueryWrapper<DigitalProduct>()
                .eq(DigitalProduct::getStatus, "PUBLISHED")
                .orderByAsc(DigitalProduct::getSortOrder);
        if (type != null) q.eq(DigitalProduct::getProductType, type);
        productMapper.selectPage(p, q);
        return Result.ok(p.getRecords());
    }

    /**
     * Get product details.
     */
    @GetMapping("/api/products/{slug}")
    public Result<Map<String, Object>> getProduct(@PathVariable String slug,
                                                   @RequestParam(required = false) Long memberId) {
        DigitalProduct product = productMapper.selectOne(
                new LambdaQueryWrapper<DigitalProduct>().eq(DigitalProduct::getSlug, slug));
        if (product == null) return Result.fail("Product not found");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("product", product);

        // Check if member has purchased
        if (memberId != null) {
            ProductPurchase purchase = purchaseMapper.selectOne(
                    new LambdaQueryWrapper<ProductPurchase>()
                            .eq(ProductPurchase::getProductId, product.getId())
                            .eq(ProductPurchase::getMemberId, memberId)
                            .eq(ProductPurchase::getStatus, "PAID"));
            result.put("purchased", purchase != null);
            result.put("downloadToken", purchase != null ? purchase.getDownloadToken() : null);
        } else {
            result.put("purchased", false);
        }

        return Result.ok(result);
    }

    /**
     * Purchase a product.
     */
    @PostMapping("/api/products/{productId}/purchase")
    public Result<Map<String, Object>> purchase(@PathVariable Long productId,
                                                 @RequestBody PurchaseRequest req) {
        DigitalProduct product = productMapper.selectById(productId);
        if (product == null || !"PUBLISHED".equals(product.getStatus())) {
            return Result.fail("Product not available");
        }

        Member member = memberMapper.selectById(req.getMemberId());
        if (member == null) return Result.fail("Member not found");

        // Check already purchased
        Long existing = purchaseMapper.selectCount(
                new LambdaQueryWrapper<ProductPurchase>()
                        .eq(ProductPurchase::getProductId, productId)
                        .eq(ProductPurchase::getMemberId, req.getMemberId())
                        .eq(ProductPurchase::getStatus, "PAID"));
        if (existing > 0) return Result.fail("Already purchased");

        // For free products, complete immediately
        String token = UUID.randomUUID().toString().replace("-", "");
        String orderNo = "DIG" + System.currentTimeMillis() + String.format("%04d", new Random().nextInt(10000));

        ProductPurchase purchase = new ProductPurchase();
        purchase.setProductId(productId);
        purchase.setMemberId(req.getMemberId());
        purchase.setOrderNo(orderNo);
        purchase.setAmountCents(product.getPriceCents());
        purchase.setCurrency(product.getCurrency());
        purchase.setStatus("PAID"); // Mock mode: directly mark as paid
        purchase.setDownloadToken(token);
        purchase.setDownloadCount(0);
        purchase.setMaxDownloads(5);
        purchase.setExpiresAt(LocalDateTime.now().plusDays(365));
        purchaseMapper.insert(purchase);

        // Update download count for product
        if (product.getPriceCents() == 0) {
            productMapper.update(null, new LambdaUpdateWrapper<DigitalProduct>()
                    .eq(DigitalProduct::getId, productId)
                    .setSql("download_count = download_count + 1"));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderNo", orderNo);
        result.put("status", purchase.getStatus());
        result.put("downloadToken", product.getPriceCents() == 0 ? token : null);
        // If paid, frontend should redirect to payment
        if (product.getPriceCents() > 0) {
            result.put("paymentRequired", true);
            result.put("amountCents", product.getPriceCents());
        }
        return Result.ok(result);
    }

    /**
     * Download a product (token-verified).
     */
    @GetMapping("/api/products/download/{token}")
    public Result<Map<String, Object>> download(@PathVariable String token) {
        ProductPurchase purchase = purchaseMapper.selectOne(
                new LambdaQueryWrapper<ProductPurchase>()
                        .eq(ProductPurchase::getDownloadToken, token)
                        .eq(ProductPurchase::getStatus, "PAID"));

        if (purchase == null) return Result.fail("Invalid download token");
        if (purchase.getExpiresAt() != null && purchase.getExpiresAt().isBefore(LocalDateTime.now())) {
            return Result.fail("Download link expired");
        }
        if (purchase.getDownloadCount() >= purchase.getMaxDownloads()) {
            return Result.fail("Download limit reached (" + purchase.getMaxDownloads() + " downloads max)");
        }

        DigitalProduct product = productMapper.selectById(purchase.getProductId());
        if (product == null || product.getFileUrl() == null) return Result.fail("File not available");

        // Increment download count
        purchaseMapper.update(null, new LambdaUpdateWrapper<ProductPurchase>()
                .eq(ProductPurchase::getId, purchase.getId())
                .setSql("download_count = download_count + 1"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fileUrl", product.getFileUrl());
        result.put("title", product.getTitle());
        result.put("remainingDownloads", purchase.getMaxDownloads() - purchase.getDownloadCount() - 1);
        return Result.ok(result);
    }

    /**
     * List member's purchased products.
     */
    @GetMapping("/api/member/{memberId}/products")
    public Result<List<Map<String, Object>>> memberProducts(@PathVariable Long memberId) {
        List<ProductPurchase> purchases = purchaseMapper.selectList(
                new LambdaQueryWrapper<ProductPurchase>()
                        .eq(ProductPurchase::getMemberId, memberId)
                        .eq(ProductPurchase::getStatus, "PAID")
                        .orderByDesc(ProductPurchase::getCreatedAt));

        List<Map<String, Object>> result = new ArrayList<>();
        for (ProductPurchase p : purchases) {
            DigitalProduct product = productMapper.selectById(p.getProductId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("purchase", p);
            item.put("product", product);
            result.add(item);
        }
        return Result.ok(result);
    }

    // ============ Admin ============

    @PostMapping("/api/admin/products")
    public Result<DigitalProduct> createProduct(@RequestBody DigitalProduct product) {
        productMapper.insert(product);
        return Result.ok(product);
    }

    @PutMapping("/api/admin/products/{id}")
    public Result<Void> updateProduct(@PathVariable Long id, @RequestBody DigitalProduct product) {
        product.setId(id);
        productMapper.updateById(product);
        return Result.ok();
    }

    @DeleteMapping("/api/admin/products/{id}")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        productMapper.deleteById(id);
        return Result.ok();
    }

    @GetMapping("/api/admin/products")
    public Result<List<DigitalProduct>> adminListProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        var p = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<DigitalProduct>(page, size);
        productMapper.selectPage(p, new LambdaQueryWrapper<DigitalProduct>()
                .orderByDesc(DigitalProduct::getCreatedAt));
        return Result.ok(p.getRecords());
    }

    @GetMapping("/api/admin/products/{id}/purchases")
    public Result<List<ProductPurchase>> productPurchases(@PathVariable Long id) {
        return Result.ok(purchaseMapper.selectList(
                new LambdaQueryWrapper<ProductPurchase>()
                        .eq(ProductPurchase::getProductId, id)
                        .orderByDesc(ProductPurchase::getCreatedAt)));
    }

    @Data
    public static class PurchaseRequest {
        private Long memberId;
    }
}
