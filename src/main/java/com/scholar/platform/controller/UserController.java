package com.scholar.platform.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scholar.platform.dto.ApiResponse;
import com.scholar.platform.dto.AppealRequest;
import com.scholar.platform.dto.CertificationRequest;
import com.scholar.platform.dto.UpdateUserRequest;
import com.scholar.platform.entity.ScholarCertification;
import com.scholar.platform.entity.User;
import com.scholar.platform.entity.UserAppeal;
import com.scholar.platform.service.AppealService;
import com.scholar.platform.service.CertificationService;
import com.scholar.platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户信息查询和管理接口")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

  private final UserService userService;
  private final CertificationService certificationService;
  private final AppealService appealService;
  private final ObjectMapper objectMapper;

  private String currentUserEmail() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || authentication.getName() == null) {
      throw new RuntimeException("未登录");
    }
    return authentication.getName();
  }

  @GetMapping("/{id}")
  @Operation(summary = "根据ID查询用户", description = "获取指定用户的详细信息")
  public ResponseEntity<ApiResponse<User>> getUserById(
      @Parameter(description = "用户ID") @PathVariable String id) {
    return userService.findById(id)
        .map(user -> ResponseEntity.ok(ApiResponse.success(user)))
        .orElse(ResponseEntity.ok(ApiResponse.error(404, "用户不存在")));
  }

  @GetMapping("/email/{email}")
  @Operation(summary = "根据邮箱查询用户", description = "通过邮箱地址查找用户")
  public ResponseEntity<ApiResponse<User>> getUserByEmail(
      @Parameter(description = "邮箱地址") @PathVariable String email) {
    return userService.findByEmail(email)
        .map(user -> ResponseEntity.ok(ApiResponse.success(user)))
        .orElse(ResponseEntity.ok(ApiResponse.error(404, "用户不存在")));
  }

  @GetMapping
  @Operation(summary = "获取所有用户", description = "查询所有用户列表(仅管理员)")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
    List<User> users = userService.findAll();
    return ResponseEntity.ok(ApiResponse.success(users));
  }

  @GetMapping("/me")
  @Operation(summary = "获取当前登录用户信息")
  public ResponseEntity<ApiResponse<User>> getMe() {
    User user = userService.getByEmailOrThrow(currentUserEmail());
    return ResponseEntity.ok(ApiResponse.success(user));
  }

  @PutMapping("/me")
  @Operation(summary = "更新当前登录用户信息")
  public ResponseEntity<ApiResponse<User>> updateMe(@Validated @RequestBody UpdateUserRequest request) {
    User user = userService.getByEmailOrThrow(currentUserEmail());
    if (request.getUsername() != null) {
      user.setUsername(request.getUsername());
    }
    if (request.getEmail() != null) {
      user.setEmail(request.getEmail());
    }
    if (request.getPreferences() != null) {
      try {
        user.setPreferences(objectMapper.writeValueAsString(request.getPreferences()));
      } catch (JsonProcessingException e) {
        throw new RuntimeException("偏好设置格式不正确");
      }
    }
    User updated = userService.save(user);
    return ResponseEntity.ok(ApiResponse.success("更新成功", updated));
  }

  @PostMapping("/me/certification")
  @Operation(summary = "提交学者认证申请")
  public ResponseEntity<ApiResponse<ScholarCertification>> submitCertification(
      @Validated @RequestBody CertificationRequest request) {
    User user = userService.getByEmailOrThrow(currentUserEmail());
    ScholarCertification certification = certificationService.submitCertification(user.getId(), request);
    return ResponseEntity.accepted().body(ApiResponse.success("认证已提交", certification));
  }

  @GetMapping("/me/certification")
  @Operation(summary = "查看自己的认证申请状态")
  public ResponseEntity<ApiResponse<Map<String, Object>>> getMyCertificationStatus() {
    User user = userService.getByEmailOrThrow(currentUserEmail());
    ScholarCertification latest = certificationService.getLatestByUser(user.getId());
    Map<String, Object> resp = new HashMap<>();
    if (latest == null) {
      resp.put("status", user.getCertificationStatus().name().toLowerCase());
    } else {
      resp.put("status", latest.getStatus().name().toLowerCase());
      if (latest.getRejectionReason() != null) {
        resp.put("reason", latest.getRejectionReason());
      }
    }
    return ResponseEntity.ok(ApiResponse.success(resp));
  }

  @PostMapping("/me/appeal")
  @Operation(summary = "发起申诉（身份冒用或成果冒领）")
  public ResponseEntity<ApiResponse<UserAppeal>> createAppeal(@Validated @RequestBody AppealRequest request) {
    User user = userService.getByEmailOrThrow(currentUserEmail());
    UserAppeal appeal = appealService.createAppeal(user.getId(), request);
    return ResponseEntity.accepted().body(ApiResponse.success("申诉已提交", appeal));
  }
}
