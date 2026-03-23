package com.blog.server.controller;

import com.blog.server.common.Result;
import com.blog.server.dto.request.LoginRequest;
import com.blog.server.dto.request.RefreshTokenRequest;
import com.blog.server.dto.request.RegisterRequest;
import com.blog.server.dto.request.Verify2faRequest;
import com.blog.server.dto.response.LoginResponse;
import com.blog.server.entity.SysUser;
import com.blog.server.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                       HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        return Result.ok(authService.login(request, ip));
    }

    @PostMapping("/verify-2fa")
    public Result<LoginResponse> verify2fa(@Valid @RequestBody Verify2faRequest request,
                                           HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        return Result.ok(authService.verify2fa(request, ip));
    }

    @PostMapping("/refresh")
    public Result<LoginResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return Result.ok(authService.refreshToken(request));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@AuthenticationPrincipal SysUser user) {
        authService.logout(user.getId());
        return Result.ok();
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return Result.ok();
    }

    @GetMapping("/me")
    public Result<SysUser> getCurrentUser(@AuthenticationPrincipal SysUser user) {
        return Result.ok(authService.getCurrentUser(user.getId()));
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For may contain multiple IPs — take the first
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
