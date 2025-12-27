package com.scholar.platform.controller;

import com.scholar.platform.dto.ApiResponse;
import com.scholar.platform.dto.ForgotPasswordRequest;
import com.scholar.platform.dto.LoginRequest;
import com.scholar.platform.dto.LoginResponse;
import com.scholar.platform.dto.RegisterRequest;
import com.scholar.platform.dto.RegisterResponse;
import com.scholar.platform.dto.ResetPasswordRequest;
import com.scholar.platform.entity.User;
import com.scholar.platform.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "用户注册、登录相关接口")
public class AuthController {

  private final AuthService authService;

  @PostMapping("/register")
  @Operation(summary = "用户注册", description = "创建新用户账号")
  public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
    User user = authService.register(request);
    RegisterResponse resp = new RegisterResponse(user.getId(), user.getUsername(), user.getEmail());
    return ResponseEntity.status(201).body(ApiResponse.success(201, "注册成功", resp));
  }

  @PostMapping("/login")
  @Operation(summary = "用户登录", description = "使用用户名或邮箱 + 密码登录，返回JWT token")
  public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
    LoginResponse response = authService.login(request);
    return ResponseEntity.ok(ApiResponse.success("登录成功", response));
  }

  @PostMapping("/forgot-password")
  @Operation(summary = "忘记密码", description = "发送验证码到邮箱")
  public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
    authService.forgotPassword(request);
    return ResponseEntity.ok(ApiResponse.success("验证码已发送", null));
  }

  @PostMapping("/reset-password")
  @Operation(summary = "重置密码", description = "验证邮箱与验证码并重置密码")
  public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    authService.resetPassword(request);
    return ResponseEntity.ok(ApiResponse.success("密码重置成功", null));
  }
}
