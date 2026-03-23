package com.blog.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.blog.server.common.Result;
import com.blog.server.entity.*;
import com.blog.server.mapper.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * J1: Membership system — registration, login, profile, plans, member-only content.
 */
@RestController
@RequiredArgsConstructor
public class MembershipController {

    private final MemberMapper memberMapper;
    private final MembershipPlanMapper planMapper;
    private final PasswordEncoder passwordEncoder;

    // ============ Public Member Auth ============

    /**
     * Register a new member.
     */
    @PostMapping("/api/member/register")
    public Result<Map<String, Object>> register(@RequestBody MemberRegisterRequest req) {
        Member existing = memberMapper.selectOne(
                new LambdaQueryWrapper<Member>().eq(Member::getEmail, req.getEmail()));
        if (existing != null) {
            return Result.fail("Email already registered");
        }

        Member member = new Member();
        member.setEmail(req.getEmail());
        member.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        member.setNickname(req.getNickname() != null ? req.getNickname() : req.getEmail().split("@")[0]);
        member.setTier("FREE");
        member.setStatus("ACTIVE");
        member.setLoginProvider("email");
        memberMapper.insert(member);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", member.getId());
        result.put("email", member.getEmail());
        result.put("tier", member.getTier());
        return Result.ok(result);
    }

    /**
     * Member login.
     */
    @PostMapping("/api/member/login")
    public Result<Map<String, Object>> login(@RequestBody MemberLoginRequest req) {
        Member member = memberMapper.selectOne(
                new LambdaQueryWrapper<Member>().eq(Member::getEmail, req.getEmail()));
        if (member == null || !passwordEncoder.matches(req.getPassword(), member.getPasswordHash())) {
            return Result.fail("Invalid email or password");
        }
        if (!"ACTIVE".equals(member.getStatus())) {
            return Result.fail("Account is suspended");
        }

        // Update last login
        memberMapper.update(null, new LambdaUpdateWrapper<Member>()
                .eq(Member::getId, member.getId())
                .set(Member::getLastLoginAt, LocalDateTime.now()));

        // Check if premium has expired
        String currentTier = member.getTier();
        if ("PREMIUM".equals(currentTier) && member.getTierExpiresAt() != null
                && member.getTierExpiresAt().isBefore(LocalDateTime.now())) {
            currentTier = "FREE";
            memberMapper.update(null, new LambdaUpdateWrapper<Member>()
                    .eq(Member::getId, member.getId())
                    .set(Member::getTier, "FREE"));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", member.getId());
        result.put("email", member.getEmail());
        result.put("nickname", member.getNickname());
        result.put("avatar", member.getAvatar());
        result.put("tier", currentTier);
        result.put("tierExpiresAt", member.getTierExpiresAt());
        // In production, return a JWT here
        result.put("token", "member_" + member.getId() + "_" + System.currentTimeMillis());
        return Result.ok(result);
    }

    /**
     * Get member profile.
     */
    @GetMapping("/api/member/{id}/profile")
    public Result<Map<String, Object>> profile(@PathVariable Long id) {
        Member member = memberMapper.selectById(id);
        if (member == null) return Result.fail("Member not found");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", member.getId());
        result.put("email", member.getEmail());
        result.put("nickname", member.getNickname());
        result.put("avatar", member.getAvatar());
        result.put("tier", member.getTier());
        result.put("tierExpiresAt", member.getTierExpiresAt());
        result.put("createdAt", member.getCreatedAt());
        return Result.ok(result);
    }

    /**
     * List available membership plans.
     */
    @GetMapping("/api/membership/plans")
    public Result<List<MembershipPlan>> listPlans() {
        return Result.ok(planMapper.selectList(
                new LambdaQueryWrapper<MembershipPlan>()
                        .eq(MembershipPlan::getIsActive, true)
                        .orderByAsc(MembershipPlan::getSortOrder)));
    }

    /**
     * Check if a member can access an article based on visibility.
     */
    @GetMapping("/api/member/access-check")
    public Result<Map<String, Object>> checkAccess(
            @RequestParam String visibility,
            @RequestParam(required = false) Long memberId) {

        Map<String, Object> result = new LinkedHashMap<>();

        if ("PUBLIC".equals(visibility)) {
            result.put("allowed", true);
            result.put("reason", "Public content");
            return Result.ok(result);
        }

        if (memberId == null) {
            result.put("allowed", false);
            result.put("reason", "LOGIN_REQUIRED".equals(visibility) ? "Login required" : "Membership required");
            result.put("requiredAction", "login");
            return Result.ok(result);
        }

        Member member = memberMapper.selectById(memberId);
        if (member == null || !"ACTIVE".equals(member.getStatus())) {
            result.put("allowed", false);
            result.put("reason", "Invalid account");
            return Result.ok(result);
        }

        if ("LOGIN_REQUIRED".equals(visibility)) {
            result.put("allowed", true);
            return Result.ok(result);
        }

        if ("MEMBER_ONLY".equals(visibility) || "PREMIUM_ONLY".equals(visibility)) {
            boolean isPremium = "PREMIUM".equals(member.getTier())
                    && (member.getTierExpiresAt() == null || member.getTierExpiresAt().isAfter(LocalDateTime.now()));

            if ("MEMBER_ONLY".equals(visibility)) {
                result.put("allowed", true); // Any logged-in member
                return Result.ok(result);
            }

            if (isPremium) {
                result.put("allowed", true);
            } else {
                result.put("allowed", false);
                result.put("reason", "Premium membership required");
                result.put("requiredAction", "upgrade");
            }
            return Result.ok(result);
        }

        result.put("allowed", true);
        return Result.ok(result);
    }

    // ============ Admin ============

    /**
     * Admin: list all members.
     */
    @GetMapping("/api/admin/members")
    public Result<List<Member>> listMembers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        var p = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<Member>(page, size);
        memberMapper.selectPage(p, new LambdaQueryWrapper<Member>().orderByDesc(Member::getCreatedAt));
        return Result.ok(p.getRecords());
    }

    /**
     * Admin: manage membership plans.
     */
    @PutMapping("/api/admin/membership/plan/{id}")
    public Result<Void> updatePlan(@PathVariable Long id, @RequestBody MembershipPlan plan) {
        plan.setId(id);
        planMapper.updateById(plan);
        return Result.ok();
    }

    @PostMapping("/api/admin/membership/plan")
    public Result<MembershipPlan> createPlan(@RequestBody MembershipPlan plan) {
        planMapper.insert(plan);
        return Result.ok(plan);
    }

    // ============ DTOs ============

    @Data
    public static class MemberRegisterRequest {
        private String email;
        private String password;
        private String nickname;
    }

    @Data
    public static class MemberLoginRequest {
        private String email;
        private String password;
    }
}
