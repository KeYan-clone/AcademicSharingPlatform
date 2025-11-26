package com.scholar.platform.controller;

import com.scholar.platform.dto.ApiResponse;
import com.scholar.platform.dto.ScholarDTO;
import com.scholar.platform.service.ScholarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/scholars")
@RequiredArgsConstructor
@Tag(name = "学者管理", description = "学者信息查询和管理接口")
@SecurityRequirement(name = "Bearer Authentication")
public class ScholarController {

  private final ScholarService scholarService;

  @GetMapping("/search")
  @Operation(summary = "搜索学者", description = "根据姓名模糊搜索学者")
  public ResponseEntity<ApiResponse<List<ScholarDTO>>> searchScholarsByName(
      @Parameter(description = "学者姓名关键词") @RequestParam String name) {
    List<ScholarDTO> scholars = scholarService.searchByName(name);
    return ResponseEntity.ok(ApiResponse.success(scholars));
  }

  @GetMapping("/{userId}")
  @Operation(summary = "获取学者主页", description = "查看指定学者的公开资料")
  public ResponseEntity<ApiResponse<ScholarDTO>> getScholarProfile(
      @Parameter(description = "学者用户ID") @PathVariable String userId) {
    ScholarDTO scholar = scholarService.getScholarProfile(userId);
    return ResponseEntity.ok(ApiResponse.success(scholar));
  }

  @PutMapping("/profile")
  @Operation(summary = "更新学者资料", description = "修改当前登录学者的公开资料")
  public ResponseEntity<ApiResponse<ScholarDTO>> updateScholarProfile(
      Authentication authentication,
      @RequestBody ScholarDTO dto) {
    String userId = authentication.getName(); // Email from JWT
    ScholarDTO updated = scholarService.updateScholarProfile(userId, dto);
    return ResponseEntity.ok(ApiResponse.success("更新成功", updated));
  }
}
