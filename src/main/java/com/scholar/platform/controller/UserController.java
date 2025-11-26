package com.scholar.platform.controller;

import com.scholar.platform.dto.ApiResponse;
import com.scholar.platform.entity.User;
import com.scholar.platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户信息查询和管理接口")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

  private final UserService userService;

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

  @PutMapping("/{id}")
  @Operation(summary = "更新用户信息", description = "修改用户基本信息")
  public ResponseEntity<ApiResponse<User>> updateUser(
      @Parameter(description = "用户ID") @PathVariable String id,
      @RequestBody User user) {
    user.setId(id);
    User updated = userService.save(user);
    return ResponseEntity.ok(ApiResponse.success("更新成功", updated));
  }
}
