package com.scholar.platform.controller;

import com.scholar.platform.dto.ApiResponse;
import com.scholar.platform.dto.CertificationRequest;
import com.scholar.platform.entity.ScholarCertification;
import com.scholar.platform.service.CertificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/certifications")
@RequiredArgsConstructor
@Tag(name = "学者认证", description = "学者身份认证申请和审核接口")
@SecurityRequirement(name = "Bearer Authentication")
public class CertificationController {

  private final CertificationService certificationService;

  @PostMapping("/submit")
  @Operation(summary = "提交认证申请", description = "用户提交学者身份认证材料")
  public ResponseEntity<ApiResponse<ScholarCertification>> submitCertification(
      Authentication authentication,
      @Valid @RequestBody CertificationRequest request) {
    String userId = authentication.getName();
    ScholarCertification certification = certificationService.submitCertification(userId, request);
    return ResponseEntity.ok(ApiResponse.success("认证申请已提交", certification));
  }

  @GetMapping("/pending")
  @Operation(summary = "获取待审核申请", description = "管理员查看所有待审核的认证申请")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<List<ScholarCertification>>> getPendingCertifications() {
    List<ScholarCertification> certifications = certificationService.getPendingCertifications();
    return ResponseEntity.ok(ApiResponse.success(certifications));
  }

  @PostMapping("/{certificationId}/approve")
  @Operation(summary = "批准认证申请", description = "管理员批准学者认证")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<ScholarCertification>> approveCertification(
      @Parameter(description = "认证申请ID") @PathVariable String certificationId,
      Authentication authentication) {
    String adminId = authentication.getName();
    ScholarCertification certification = certificationService.approveCertification(certificationId, adminId);
    return ResponseEntity.ok(ApiResponse.success("认证已批准", certification));
  }

  @PostMapping("/{certificationId}/reject")
  @Operation(summary = "驳回认证申请", description = "管理员驳回学者认证并说明理由")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<ScholarCertification>> rejectCertification(
      @Parameter(description = "认证申请ID") @PathVariable String certificationId,
      @Parameter(description = "驳回理由") @RequestParam String reason,
      Authentication authentication) {
    String adminId = authentication.getName();
    ScholarCertification certification = certificationService.rejectCertification(certificationId, adminId, reason);
    return ResponseEntity.ok(ApiResponse.success("认证已驳回", certification));
  }
}
