package com.blog.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class Verify2faRequest {

    @NotBlank(message = "Temp token is required")
    private String tempToken;

    @NotBlank(message = "Verification code is required")
    private String code;
}
