package com.scholar.platform.controller;

import com.scholar.platform.dto.ApiResponse;
import com.scholar.platform.dto.LoginRequest;
import com.scholar.platform.dto.LoginResponse;
import com.scholar.platform.dto.RegisterRequest;
import com.scholar.platform.entity.User;
import com.scholar.platform.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "用户注册、登录相关接口")
public class AuthController {

  private final AuthService authService;

  @PostMapping("/register")
  @Operation(summary = "用户注册", description = "创建新用户账号")
  public ResponseEntity<ApiResponse<User>> register(@Valid @RequestBody RegisterRequest request) {
    User user = authService.register(request);
    return ResponseEntity.ok(ApiResponse.success("注册成功", user));
  }

  @PostMapping("/login")
  @Operation(summary = "用户登录", description = "使用邮箱和密码登录，返回JWT token")
  public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
    LoginResponse response = authService.login(request);
    return ResponseEntity.ok(ApiResponse.success("登录成功", response));
  }
}
