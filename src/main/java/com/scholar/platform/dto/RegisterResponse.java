package com.scholar.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "注册响应")
public class RegisterResponse {

  @Schema(description = "用户ID")
  private String userId;

  @Schema(description = "用户名")
  private String username;

  @Schema(description = "邮箱")
  private String email;
}
