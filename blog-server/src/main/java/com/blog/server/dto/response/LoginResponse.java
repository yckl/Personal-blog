package com.blog.server.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class LoginResponse {

    private String token;
    private String refreshToken;
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private List<String> roles;
    private List<String> permissions;

    /** If true, the client must call verify-2fa before accessing the system */
    private Boolean requireTwoFactor;

    /** Short-lived token used only for the 2FA verification step */
    private String tempToken;
}
