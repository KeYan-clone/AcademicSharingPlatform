package com.scholar.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "重置密码请求")
public class ResetPasswordRequest {

  @NotBlank(message = "邮箱不能为空")
  @Email(message = "邮箱格式不正确")
  @Schema(description = "邮箱", example = "john@example.com")
  private String email;

  @NotBlank(message = "验证码不能为空")
  @Schema(description = "验证码", example = "123456")
  private String verificationCode;

  @NotBlank(message = "新密码不能为空")
  @Size(min = 6, max = 20, message = "密码长度必须在6-20之间")
  @Schema(description = "新密码", example = "newPassword123")
  private String newPassword;
}
