package com.blog.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.server.common.Result;
import com.blog.server.entity.*;
import com.blog.server.mapper.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin user and role management endpoints — full RBAC CRUD.
 */
@RestController
@RequiredArgsConstructor
public class UserManagementController {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final PasswordEncoder passwordEncoder;

    // ============ User — Read ============

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

    // ============ User — Create ============

    @PostMapping("/api/admin/users")
    public Result<Map<String, Object>> createUser(@RequestBody UserCreateRequest req) {
        Long exists = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, req.getUsername()));
        if (exists > 0) return Result.fail("用户名已存在");

        SysUser user = new SysUser();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setNickname(req.getNickname());
        user.setEmail(req.getEmail());
        user.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        userMapper.insert(user);

        if (req.getRoleIds() != null) {
            for (Long roleId : req.getRoleIds()) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(user.getId());
                ur.setRoleId(roleId);
                userRoleMapper.insert(ur);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        return Result.ok(result);
    }

    // ============ User — Update ============

    @PutMapping("/api/admin/users/{id}")
    public Result<Void> updateUser(@PathVariable Long id, @RequestBody UserUpdateRequest req) {
        SysUser user = userMapper.selectById(id);
        if (user == null) return Result.fail("用户不存在");

        if (req.getNickname() != null) user.setNickname(req.getNickname());
        if (req.getEmail() != null) user.setEmail(req.getEmail());
        if (req.getStatus() != null) user.setStatus(req.getStatus());
        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(req.getPassword()));
        }
        userMapper.updateById(user);

        if (req.getRoleIds() != null) {
            userRoleMapper.delete(
                    new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
            for (Long roleId : req.getRoleIds()) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(id);
                ur.setRoleId(roleId);
                userRoleMapper.insert(ur);
            }
        }
        return Result.ok();
    }

    // ============ User — Delete ============

    @DeleteMapping("/api/admin/users/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userMapper.deleteById(id);
        userRoleMapper.delete(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
        return Result.ok();
    }

    // ============ Role — Read ============

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
            m.put("roleKey", role.getRoleKey());
            m.put("description", role.getDescription());
            m.put("permissions", rolePermsMap.getOrDefault(role.getId(), Collections.emptyList()));
            m.put("createdAt", role.getCreatedAt());
            result.add(m);
        }
        return Result.ok(result);
    }

    // ============ Role — Create ============

    @PostMapping("/api/admin/roles")
    public Result<Map<String, Object>> createRole(@RequestBody RoleRequest req) {
        Long exists = roleMapper.selectCount(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleKey, req.getRoleKey()));
        if (exists > 0) return Result.fail("角色标识已存在");

        SysRole role = new SysRole();
        role.setRoleName(req.getRoleName());
        role.setRoleKey(req.getRoleKey());
        role.setDescription(req.getDescription());
        roleMapper.insert(role);

        if (req.getPermissionIds() != null) {
            for (Long permId : req.getPermissionIds()) {
                SysRolePermission rp = new SysRolePermission();
                rp.setRoleId(role.getId());
                rp.setPermissionId(permId);
                rolePermissionMapper.insert(rp);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", role.getId());
        result.put("roleName", role.getRoleName());
        return Result.ok(result);
    }

    // ============ Role — Update ============

    @PutMapping("/api/admin/roles/{id}")
    public Result<Void> updateRole(@PathVariable Long id, @RequestBody RoleRequest req) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) return Result.fail("角色不存在");

        if (req.getRoleName() != null) role.setRoleName(req.getRoleName());
        if (req.getRoleKey() != null) role.setRoleKey(req.getRoleKey());
        if (req.getDescription() != null) role.setDescription(req.getDescription());
        roleMapper.updateById(role);

        if (req.getPermissionIds() != null) {
            rolePermissionMapper.delete(
                    new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, id));
            for (Long permId : req.getPermissionIds()) {
                SysRolePermission rp = new SysRolePermission();
                rp.setRoleId(id);
                rp.setPermissionId(permId);
                rolePermissionMapper.insert(rp);
            }
        }
        return Result.ok();
    }

    // ============ Role — Delete ============

    @DeleteMapping("/api/admin/roles/{id}")
    public Result<Void> deleteRole(@PathVariable Long id) {
        roleMapper.deleteById(id);
        rolePermissionMapper.delete(
                new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, id));
        userRoleMapper.delete(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, id));
        return Result.ok();
    }

    // ============ DTOs ============

    @Data
    public static class UserCreateRequest {
        private String username;
        private String password;
        private String nickname;
        private String email;
        private Integer status;
        private List<Long> roleIds;
    }

    @Data
    public static class UserUpdateRequest {
        private String password;
        private String nickname;
        private String email;
        private Integer status;
        private List<Long> roleIds;
    }

    @Data
    public static class RoleRequest {
        private String roleName;
        private String roleKey;
        private String description;
        private List<Long> permissionIds;
    }
}
