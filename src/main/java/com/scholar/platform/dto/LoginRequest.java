package com.scholar.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "用户登录请求")
public class LoginRequest {

    @NotBlank(message = "账号不能为空")
    @Schema(description = "账号（用户名或邮箱）", example = "john@example.com 或 john_doe")
    private String account;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码", example = "password123")
    private String password;
}
