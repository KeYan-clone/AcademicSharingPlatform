package com.scholar.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "更新用户信息请求")
public class UpdateUserRequest {

  @Size(min = 3, max = 50, message = "用户名长度必须在3-50之间")
  @Schema(description = "用户名", example = "john_doe")
  private String username;

  @Email(message = "邮箱格式不正确")
  @Schema(description = "邮箱", example = "john@example.com")
  private String email;

  @Schema(description = "偏好设置(JSON对象)")
  private Map<String, Object> preferences;
}
