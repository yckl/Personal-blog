package com.blog.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.server.dto.request.LoginRequest;
import com.blog.server.dto.request.RefreshTokenRequest;
import com.blog.server.dto.request.RegisterRequest;
import com.blog.server.dto.request.Verify2faRequest;
import com.blog.server.dto.response.LoginResponse;
import com.blog.server.entity.SysRole;
import com.blog.server.entity.SysUser;
import com.blog.server.entity.SysUserRole;
import com.blog.server.exception.BusinessException;
import com.blog.server.mapper.SysRoleMapper;
import com.blog.server.mapper.SysUserMapper;
import com.blog.server.mapper.SysUserRoleMapper;
import com.blog.server.security.JwtTokenProvider;
import com.blog.server.security.TotpService;
import com.blog.server.service.AuthService;
import com.blog.server.service.SystemLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final TotpService totpService;
    private final SystemLogService systemLogService;

    @Override
    public LoginResponse login(LoginRequest request, String ip) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, request.getUsername())
        );

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            // Log failed attempt
            Long failedUserId = user != null ? user.getId() : null;
            systemLogService.logLogin(failedUserId, ip, false, "Invalid username or password");
            throw new BusinessException(401, "Invalid username or password");
        }

        if (user.getStatus() != 1) {
            systemLogService.logLogin(user.getId(), ip, false, "Account is disabled");
            throw new BusinessException(403, "Account is disabled");
        }

        // Check if 2FA is enabled
        if (Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            // Return a temp token — the client must call /verify-2fa next
            String tempToken = jwtTokenProvider.generateTempToken(user.getId(), user.getUsername());
            LoginResponse response = new LoginResponse();
            response.setRequireTwoFactor(true);
            response.setTempToken(tempToken);
            return response;
        }

        // No 2FA — issue full tokens
        return buildFullLoginResponse(user, ip);
    }

    @Override
    public LoginResponse verify2fa(Verify2faRequest request, String ip) {
        String tempToken = request.getTempToken();

        if (!jwtTokenProvider.validateToken(tempToken, "temp")) {
            throw new BusinessException(401, "Invalid or expired verification token");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(tempToken);
        SysUser user = sysUserMapper.selectById(userId);

        if (user == null) {
            throw new BusinessException(401, "User not found");
        }

        if (!totpService.verifyCode(user.getTwoFactorSecret(), request.getCode())) {
            systemLogService.logLogin(userId, ip, false, "Invalid 2FA code");
            throw new BusinessException(401, "Invalid verification code");
        }

        return buildFullLoginResponse(user, ip);
    }

    @Override
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken, "refresh")) {
            throw new BusinessException(401, "Invalid or expired refresh token");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || user.getStatus() != 1) {
            throw new BusinessException(401, "User not found or disabled");
        }

        String newAccessToken = jwtTokenProvider.generateToken(userId, username);

        List<String> roles = sysUserMapper.selectRoleKeysByUserId(userId);
        List<String> permissions = sysUserMapper.selectPermissionCodesByUserId(userId);

        LoginResponse response = new LoginResponse();
        response.setToken(newAccessToken);
        response.setRefreshToken(refreshToken); // Return the same refresh token
        response.setUserId(userId);
        response.setUsername(username);
        response.setNickname(user.getNickname());
        response.setAvatar(user.getAvatar());
        response.setRoles(roles);
        response.setPermissions(permissions);
        return response;
    }

    @Override
    public void logout(Long userId) {
        // Client-side token removal is sufficient.
        // For additional security, we could blacklist the token in Redis here.
    }

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        // Check if username exists
        Long count = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, request.getUsername())
        );
        if (count > 0) {
            throw new BusinessException("Username already exists");
        }

        // Create user
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setEmail(request.getEmail());
        user.setStatus(1);
        user.setTwoFactorEnabled(false);
        sysUserMapper.insert(user);

        // Assign ADMIN role (first registered user is admin)
        SysRole adminRole = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleKey, "ADMIN")
        );
        if (adminRole != null) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(adminRole.getId());
            sysUserRoleMapper.insert(userRole);
        }
    }

    @Override
    public SysUser getCurrentUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "User not found");
        }
        user.setPassword(null); // Don't expose password
        user.setTwoFactorSecret(null); // Don't expose 2FA secret

        // Attach roles & permissions so frontend sidebar works after page refresh
        List<String> userRoles = sysUserMapper.selectRoleKeysByUserId(userId);
        List<String> userPermissions = sysUserMapper.selectPermissionCodesByUserId(userId);
        user.setRoles(userRoles);
        user.setPermissions(userPermissions);

        return user;
    }

    // ---- Private helpers ----

    private LoginResponse buildFullLoginResponse(SysUser user, String ip) {
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getUsername());
        List<String> roles = sysUserMapper.selectRoleKeysByUserId(user.getId());
        List<String> permissions = sysUserMapper.selectPermissionCodesByUserId(user.getId());

        // Update last login
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(ip);
        sysUserMapper.updateById(user);

        // Log successful login
        systemLogService.logLogin(user.getId(), ip, true, null);

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setRefreshToken(refreshToken);
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setAvatar(user.getAvatar());
        response.setRoles(roles);
        response.setPermissions(permissions);
        response.setRequireTwoFactor(false);
        return response;
    }
}
