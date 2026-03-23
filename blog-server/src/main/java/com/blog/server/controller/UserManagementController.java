package com.blog.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.server.common.Result;
import com.blog.server.entity.*;
import com.blog.server.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin user and role management endpoints.
 */
@RestController
@RequiredArgsConstructor
public class UserManagementController {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;

    /**
     * List all admin users with their roles.
     */
    @GetMapping("/api/admin/users")
    public Result<List<Map<String, Object>>> listUsers() {
        List<SysUser> users = userMapper.selectList(
                new LambdaQueryWrapper<SysUser>().orderByDesc(SysUser::getCreatedAt));

        List<SysRole> allRoles = roleMapper.selectList(null);
        Map<Long, String> roleNameMap = allRoles.stream()
                .collect(Collectors.toMap(SysRole::getId, SysRole::getRoleName));

        List<SysUserRole> allUserRoles = userRoleMapper.selectList(null);
        Map<Long, List<String>> userRolesMap = allUserRoles.stream()
                .collect(Collectors.groupingBy(SysUserRole::getUserId,
                        Collectors.mapping(ur -> roleNameMap.getOrDefault(ur.getRoleId(), "UNKNOWN"),
                                Collectors.toList())));

        List<Map<String, Object>> result = new ArrayList<>();
        for (SysUser user : users) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", user.getId());
            m.put("username", user.getUsername());
            m.put("nickname", user.getNickname());
            m.put("email", user.getEmail());
            m.put("avatar", user.getAvatar());
            m.put("status", user.getStatus());
            m.put("roles", userRolesMap.getOrDefault(user.getId(), Collections.emptyList()));
            m.put("lastLoginAt", user.getLastLoginAt());
            m.put("createdAt", user.getCreatedAt());
            result.add(m);
        }
        return Result.ok(result);
    }

    /**
     * List all roles with their permissions.
     */
    @GetMapping("/api/admin/roles")
    public Result<List<Map<String, Object>>> listRoles() {
        List<SysRole> roles = roleMapper.selectList(
                new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId));

        List<SysRolePermission> allPerms = rolePermissionMapper.selectList(null);
        Map<Long, List<Long>> rolePermsMap = allPerms.stream()
                .collect(Collectors.groupingBy(SysRolePermission::getRoleId,
                        Collectors.mapping(SysRolePermission::getPermissionId, Collectors.toList())));

        List<Map<String, Object>> result = new ArrayList<>();
        for (SysRole role : roles) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", role.getId());
            m.put("name", role.getRoleName());
            m.put("description", role.getDescription());
            m.put("permissions", rolePermsMap.getOrDefault(role.getId(), Collections.emptyList()));
            m.put("createdAt", role.getCreatedAt());
            result.add(m);
        }
        return Result.ok(result);
    }
}
