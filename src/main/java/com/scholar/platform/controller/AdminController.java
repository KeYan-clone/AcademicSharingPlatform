package com.scholar.platform.controller;

import com.scholar.platform.dto.AchievementClaimReplyDTO;
import com.scholar.platform.dto.AchievementDTO;
import com.scholar.platform.dto.ApiResponse;
import com.scholar.platform.dto.PendingClaimRequestDTO;
import com.scholar.platform.entity.Achievement;
import com.scholar.platform.entity.ScholarCertification;
import com.scholar.platform.entity.UserAppeal;
import com.scholar.platform.service.AchievementService;
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
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "管理员", description = "管理员管理接口")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

  private final CertificationService certificationService;
  private final AppealService appealService;
  private final AchievementService achievementService;
  private final UserService userService;

  /**
   * 获取待审核的学者认证列表
   */
  @GetMapping("/certifications")
  @Operation(summary = "获取待审核的学者认证列表", description = "管理员查看所有待审核的学者认证申请")
  public ResponseEntity<ApiResponse<Map<String, Object>>> getCertifications(
      @Parameter(description = "认证状态") @RequestParam(required = false, defaultValue = "pending") String status) {
    List<ScholarCertification> certifications;

    if ("pending".equalsIgnoreCase(status)) {
      certifications = certificationService.getPendingCertifications();
    } else {
      certifications = certificationService.getPendingCertifications();
    }

    Map<String, Object> response = new HashMap<>();
    response.put("applications", certifications);

    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * 批准学者认证
   */
  @PostMapping("/certifications/{appId}/approve")
  @Operation(summary = "批准学者认证", description = "管理员批准学者认证申请")
  public ResponseEntity<ApiResponse<Map<String, String>>> approveCertification(
      @Parameter(description = "认证申请ID") @PathVariable String appId,
      Authentication authentication) {
    String adminId = authentication.getName();
    certificationService.approveCertification(appId, adminId);

    Map<String, String> response = new HashMap<>();
    response.put("message", "认证已批准");

    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * 驳回学者认证
   */
  @PostMapping("/certifications/{appId}/reject")
  @Operation(summary = "驳回学者认证", description = "管理员驳回学者认证申请并说明理由")
  public ResponseEntity<ApiResponse<Map<String, String>>> rejectCertification(
      @Parameter(description = "认证申请ID") @PathVariable String appId,
      @Parameter(description = "驳回理由") @RequestBody Map<String, String> requestBody,
      Authentication authentication) {
    String adminId = authentication.getName();
    String reason = requestBody.get("reason");

    certificationService.rejectCertification(appId, adminId, reason);

    Map<String, String> response = new HashMap<>();
    response.put("message", "认证已驳回");

    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * 获取待处理的申诉列表
   */
  @GetMapping("/appeals")
  @Operation(summary = "获取待处理的申诉列表", description = "管理员查看所有待处理的申诉")
  public ResponseEntity<ApiResponse<Map<String, Object>>> getAppeals(
      @Parameter(description = "申诉状态") @RequestParam(required = false, defaultValue = "pending") String status) {
    List<UserAppeal> appeals;

    if ("pending".equalsIgnoreCase(status)) {
      appeals = appealService.getPendingAppeals();
    } else {
      appeals = appealService.getPendingAppeals();
    }

    Map<String, Object> response = new HashMap<>();
    response.put("appeals", appeals);

    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * 处理申诉（批准或驳回）
   */
  @PostMapping("/appeals/{caseId}/process")
  @Operation(summary = "处理申诉", description = "管理员处理申诉（批准或驳回）")
  public ResponseEntity<ApiResponse<Map<String, String>>> processAppeal(
      @Parameter(description = "申诉ID") @PathVariable String caseId,
      @Parameter(description = "处理请求") @RequestBody Map<String, String> requestBody,
      Authentication authentication) {
    String adminId = authentication.getName();
    String action = requestBody.get("action");
    String reason = requestBody.getOrDefault("reason", "");

    appealService.processAppeal(caseId, adminId, action, reason);

    Map<String, String> response = new HashMap<>();
    response.put("message", "申诉已处理");

    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * 获取待审核的学者提交成果
   */
  @GetMapping("/achievements/pending")
  @Operation(summary = "获取待审核的学者提交成果", description = "管理员查看所有待审核的学者成果")
  public ResponseEntity<ApiResponse<Map<String, Object>>> getPendingAchievements() {
    List<AchievementDTO> pendingAchievements = achievementService.getPendingAchievements();

    Map<String, Object> response = new HashMap<>();
    response.put("pendingAchievements", pendingAchievements);

    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * 批准学者提交的成果
   */
  @PostMapping("/achievements/{achId}/approve")
  @Operation(summary = "批准学者提交的成果", description = "管理员批准学者提交的学术成果")
  public ResponseEntity<ApiResponse<Map<String, String>>> approveAchievement(
      @Parameter(description = "成果ID") @PathVariable String achId,
      Authentication authentication) {
    String adminId = authentication.getName();
    achievementService.approveAchievement(achId, adminId);

    Map<String, String> response = new HashMap<>();
    response.put("message", "成果已批准");

    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * 驳回学者提交的成果
   */
  @PostMapping("/achievements/{achId}/reject")
  @Operation(summary = "驳回学者提交的成果", description = "管理员驳回学者提交的学术成果并说明理由")
  public ResponseEntity<ApiResponse<Map<String, String>>> rejectAchievement(
      @Parameter(description = "成果ID") @PathVariable String achId,
      @Parameter(description = "驳回理由") @RequestBody Map<String, String> requestBody,
      Authentication authentication) {
    String adminId = authentication.getName();
    String reason = requestBody.get("reason");

    achievementService.rejectAchievement(achId, adminId, reason);

    Map<String, String> response = new HashMap<>();
    response.put("message", "成果已驳回");

    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * 获取所有待审核的成果认领请求
   */
  @GetMapping("/claim-requests/pending")
  @Operation(summary = "获取所有待审核的成果认领请求", description = "管理员查看所有待审核的成果认领请求")
  public ResponseEntity<ApiResponse<Map<String, Object>>> getPendingClaimRequests() {
    List<PendingClaimRequestDTO> pendingRequests = userService.getPendingClaimRequests();

    Map<String, Object> response = new HashMap<>();
    response.put("pendingClaimRequests", pendingRequests);

    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * 处理成果认领请求
   */
  @PostMapping("/claim-requests/reply")
  @Operation(summary = "回复成果认领请求", description = "管理员回复成果认领请求，可能建立作者关联")
  public ResponseEntity<ApiResponse<Map<String, String>>> approveClaimRequest(
      @Parameter(description = "认领请求回复") @RequestBody AchievementClaimReplyDTO reply
) {
    userService.replyClaimAchievement(reply.getRequestId(), reply.getIsApprove(), reply.getMessage());

    Map<String, String> response = new HashMap<>();
    response.put("message", "操作完成");

    return ResponseEntity.ok(ApiResponse.success(response));
  }

  
}