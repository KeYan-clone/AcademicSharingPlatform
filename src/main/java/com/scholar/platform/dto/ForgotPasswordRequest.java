package com.scholar.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "忘记密码请求")
public class ForgotPasswordRequest {

  @NotBlank(message = "邮箱不能为空")
  @Email(message = "邮箱格式不正确")
  @Schema(description = "邮箱", example = "john@example.com")
  private String email;
}
