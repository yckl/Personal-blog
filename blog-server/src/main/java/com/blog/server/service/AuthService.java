package com.blog.server.service;

import com.blog.server.dto.request.LoginRequest;
import com.blog.server.dto.request.RefreshTokenRequest;
import com.blog.server.dto.request.RegisterRequest;
import com.blog.server.dto.request.Verify2faRequest;
import com.blog.server.dto.response.LoginResponse;
import com.blog.server.entity.SysUser;

public interface AuthService {

    LoginResponse login(LoginRequest request, String ip);

    LoginResponse verify2fa(Verify2faRequest request, String ip);

    LoginResponse refreshToken(RefreshTokenRequest request);

    void logout(Long userId);

    void register(RegisterRequest request);

    SysUser getCurrentUser(Long userId);
}
