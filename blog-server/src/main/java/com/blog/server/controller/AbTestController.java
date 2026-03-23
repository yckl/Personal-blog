package com.blog.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.blog.server.common.Result;
import com.blog.server.entity.*;
import com.blog.server.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * V3: A/B testing — experiment management, variant assignment, conversion tracking.
 */
@RestController
@RequiredArgsConstructor
public class AbTestController {

    private final AbExperimentMapper experimentMapper;
    private final AbVariantMapper variantMapper;
    private final AbAssignmentMapper assignmentMapper;

    // ============ Public: get assignment ============

    /**
     * Get or create variant assignment for a visitor.
     * Frontend calls this to know which variant to show.
     */
    @GetMapping("/api/ab/assign")
    public Result<Map<String, Object>> assign(
            @RequestParam Long experimentId,
            @RequestParam String visitorId) {

        AbExperiment exp = experimentMapper.selectById(experimentId);
        if (exp == null || !"RUNNING".equals(exp.getStatus())) {
            return Result.ok(Map.of("assigned", false));
        }

        // Check existing assignment
        AbAssignment existing = assignmentMapper.selectOne(
                new LambdaQueryWrapper<AbAssignment>()
                        .eq(AbAssignment::getExperimentId, experimentId)
                        .eq(AbAssignment::getVisitorId, visitorId));

        if (existing != null) {
            AbVariant variant = variantMapper.selectById(existing.getVariantId());
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("assigned", true);
            result.put("variantId", existing.getVariantId());
            result.put("variantName", variant != null ? variant.getName() : null);
            result.put("config", variant != null ? variant.getConfigJson() : null);
            return Result.ok(result);
        }

        // Assign to a variant based on traffic split
        List<AbVariant> variants = variantMapper.selectList(
                new LambdaQueryWrapper<AbVariant>().eq(AbVariant::getExperimentId, experimentId));
        if (variants.isEmpty()) return Result.ok(Map.of("assigned", false));

        AbVariant selected = selectVariant(variants, visitorId);

        // Record assignment
        AbAssignment assignment = new AbAssignment();
        assignment.setExperimentId(experimentId);
        assignment.setVariantId(selected.getId());
        assignment.setVisitorId(visitorId);
        assignment.setConverted(0);
        assignmentMapper.insert(assignment);

        // Increment views
        variantMapper.update(null, new LambdaUpdateWrapper<AbVariant>()
                .eq(AbVariant::getId, selected.getId())
                .setSql("views = views + 1"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("assigned", true);
        result.put("variantId", selected.getId());
        result.put("variantName", selected.getName());
        result.put("config", selected.getConfigJson());
        return Result.ok(result);
    }

    /**
     * Record a conversion.
     */
    @PostMapping("/api/ab/convert")
    public Result<Void> convert(@RequestParam Long experimentId, @RequestParam String visitorId) {
        AbAssignment assignment = assignmentMapper.selectOne(
                new LambdaQueryWrapper<AbAssignment>()
                        .eq(AbAssignment::getExperimentId, experimentId)
                        .eq(AbAssignment::getVisitorId, visitorId));
        if (assignment == null || assignment.getConverted() == 1) return Result.ok();

        assignmentMapper.update(null, new LambdaUpdateWrapper<AbAssignment>()
                .eq(AbAssignment::getId, assignment.getId())
                .set(AbAssignment::getConverted, 1));

        variantMapper.update(null, new LambdaUpdateWrapper<AbVariant>()
                .eq(AbVariant::getId, assignment.getVariantId())
                .setSql("conversions = conversions + 1"));

        return Result.ok();
    }

    // ============ Admin ============

    @PostMapping("/api/admin/ab/experiment")
    public Result<AbExperiment> createExperiment(@RequestBody AbExperiment experiment) {
        experimentMapper.insert(experiment);
        return Result.ok(experiment);
    }

    @PutMapping("/api/admin/ab/experiment/{id}")
    public Result<Void> updateExperiment(@PathVariable Long id, @RequestBody AbExperiment experiment) {
        experiment.setId(id);
        experimentMapper.updateById(experiment);
        return Result.ok();
    }

    @GetMapping("/api/admin/ab/experiments")
    public Result<List<AbExperiment>> listExperiments() {
        return Result.ok(experimentMapper.selectList(
                new LambdaQueryWrapper<AbExperiment>().orderByDesc(AbExperiment::getCreatedAt)));
    }

    @PostMapping("/api/admin/ab/variant")
    public Result<AbVariant> createVariant(@RequestBody AbVariant variant) {
        variantMapper.insert(variant);
        return Result.ok(variant);
    }

    @GetMapping("/api/admin/ab/experiment/{id}/results")
    public Result<Map<String, Object>> results(@PathVariable Long id) {
        AbExperiment exp = experimentMapper.selectById(id);
        List<AbVariant> variants = variantMapper.selectList(
                new LambdaQueryWrapper<AbVariant>().eq(AbVariant::getExperimentId, id));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("experiment", exp);

        List<Map<String, Object>> variantResults = new ArrayList<>();
        for (AbVariant v : variants) {
            Map<String, Object> vr = new LinkedHashMap<>();
            vr.put("variant", v);
            vr.put("conversionRate", v.getViews() > 0
                    ? String.format("%.2f%%", (double) v.getConversions() / v.getViews() * 100) : "0.00%");
            variantResults.add(vr);
        }
        result.put("variants", variantResults);
        return Result.ok(result);
    }

    // ============ Internal ============

    private AbVariant selectVariant(List<AbVariant> variants, String visitorId) {
        // Deterministic assignment based on visitor hash
        int hash = Math.abs(visitorId.hashCode());
        int totalWeight = variants.stream().mapToInt(v -> v.getTrafficPercent() != null ? v.getTrafficPercent() : 50).sum();
        int point = hash % totalWeight;
        int cumulative = 0;
        for (AbVariant v : variants) {
            cumulative += (v.getTrafficPercent() != null ? v.getTrafficPercent() : 50);
            if (point < cumulative) return v;
        }
        return variants.get(0);
    }
}
