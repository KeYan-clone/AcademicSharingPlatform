package com.scholar.platform.controller;

import com.scholar.platform.dto.AchievementDTO;
import com.scholar.platform.dto.ApiResponse;
import com.scholar.platform.dto.PageResponse;
import com.scholar.platform.entity.Achievement;
import com.scholar.platform.service.AchievementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/achievements")
@RequiredArgsConstructor
@Tag(name = "学术成果", description = "学术成果查询和管理接口")
@SecurityRequirement(name = "Bearer Authentication")
public class AchievementController {

  private final AchievementService achievementService;

  @GetMapping("/search")
  @Operation(summary = "搜索成果", description = "根据标题关键词搜索学术成果")
  public ResponseEntity<ApiResponse<PageResponse<AchievementDTO>>> searchByTitle(
      @Parameter(description = "标题关键词") @RequestParam String title,
      @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size) {
    Pageable pageable = PageRequest.of(page, size);
    Page<AchievementDTO> result = achievementService.searchByTitle(title, pageable);
    return ResponseEntity.ok(ApiResponse.success(PageResponse.of(result)));
  }

  @GetMapping("/type/{type}")
  @Operation(summary = "按类型查询成果", description = "根据成果类型(PAPER/PATENT/PROJECT/AWARD)查询")
  public ResponseEntity<ApiResponse<PageResponse<AchievementDTO>>> getByType(
      @Parameter(description = "成果类型") @PathVariable Achievement.AchievementType type,
      @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size) {
    Pageable pageable = PageRequest.of(page, size);
    Page<AchievementDTO> result = achievementService.getByType(type, pageable);
    return ResponseEntity.ok(ApiResponse.success(PageResponse.of(result)));
  }

  @GetMapping("/{id}")
  @Operation(summary = "获取成果详情", description = "查看指定成果的详细信息")
  public ResponseEntity<ApiResponse<AchievementDTO>> getAchievementById(
      @Parameter(description = "成果ID") @PathVariable String id) {
    AchievementDTO achievement = achievementService.getById(id);
    return ResponseEntity.ok(ApiResponse.success(achievement));
  }
}
