package com.blog.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.server.common.Result;
import com.blog.server.entity.AuditLog;
import com.blog.server.mapper.AuditLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


import java.time.LocalDate;
import java.util.List;

/**
 * Admin audit log listing endpoint.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AuditLogController {

    private final AuditLogMapper auditLogMapper;

    @GetMapping("/audit-logs")
    public Result<List<AuditLog>> list(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resource,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int size) {

        var p = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<AuditLog>(page, size);
        LambdaQueryWrapper<AuditLog> q = new LambdaQueryWrapper<AuditLog>()
                .orderByDesc(AuditLog::getCreatedAt);

        if (action != null && !action.isEmpty()) q.eq(AuditLog::getAction, action);
        if (resource != null && !resource.isEmpty()) q.eq(AuditLog::getResource, resource);
        if (startDate != null) q.ge(AuditLog::getCreatedAt, LocalDate.parse(startDate).atStartOfDay());
        if (endDate != null) q.le(AuditLog::getCreatedAt, LocalDate.parse(endDate).atTime(23, 59, 59));

        auditLogMapper.selectPage(p, q);
        return Result.ok(p.getRecords());
    }
}
