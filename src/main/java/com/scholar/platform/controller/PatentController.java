package com.scholar.platform.controller;

import com.scholar.platform.dto.ApiResponse;
import com.scholar.platform.dto.PageResponse;
import com.scholar.platform.dto.PatentDTO;
import com.scholar.platform.service.PatentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/patent")
@RequiredArgsConstructor
@Tag(name = "专利", description = "专利检索接口")
public class PatentController {

  private final PatentService patentService;

  @GetMapping
  @Operation(summary = "检索专利", description = "支持关键词、申请年份、授权年份检索")
  public ResponseEntity<ApiResponse<PageResponse<PatentDTO>>> searchPatents(
      @Parameter(description = "关键词 - 模糊匹配") @RequestParam(required = false) String q,
      @Parameter(description = "申请年份 - 精确匹配") @RequestParam(required = false) Integer applicationYear,
      @Parameter(description = "授权年份 - 精确匹配") @RequestParam(required = false) Integer grantYear,
      @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size) {
    
    Pageable pageable = PageRequest.of(page, size);
    Page<PatentDTO> result = patentService.searchPatents(q, applicationYear, grantYear, pageable);
    return ResponseEntity.ok(ApiResponse.success(PageResponse.of(result)));
  }
}
