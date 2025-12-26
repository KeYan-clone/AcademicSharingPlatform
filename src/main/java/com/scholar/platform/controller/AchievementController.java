package com.scholar.platform.controller;
import com.scholar.platform.dto.AchievementDTO;
import com.scholar.platform.dto.ApiResponse;
import com.scholar.platform.dto.PageResponse;
import com.scholar.platform.service.AchievementService;
import com.scholar.platform.service.PaperKeywordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@Tag(name = "学术成果", description = "学术成果检索和管理接口")
public class AchievementController {

  private final AchievementService achievementService;
  private final PaperKeywordService paperKeywordService;

  @GetMapping
  @Operation(summary = "检索学术成果（支持高级检索）", 
             description = "支持关键词、学科领域、时间范围、作者和机构的组合检索。至少需要提供一个检索条件。field 为精确匹配。")
  public ResponseEntity<ApiResponse<PageResponse<AchievementDTO>>> searchAchievements(
      @Parameter(description = "关键词 - 模糊匹配（搜索标题和概念）") @RequestParam(required = false) String q,
      @Parameter(description = "学科领域/概念 - 精确匹配（从下拉列表选择）") @RequestParam(required = false) String field,
      @Parameter(description = "起始日期 (格式: yyyy-MM-dd)") @RequestParam(required = false) String startDate,
      @Parameter(description = "截止日期 (格式: yyyy-MM-dd)") @RequestParam(required = false) String endDate,
      @Parameter(description = "作者姓名 - 精确匹配") @RequestParam(required = false) String author,
      @Parameter(description = "机构名称 - 精确匹配") @RequestParam(required = false) String institution,
      @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size) {
    
    Pageable pageable = PageRequest.of(page, size);
    
    try {
      Page<AchievementDTO> result = achievementService.advancedSearch(q, field, startDate, endDate, author, institution, pageable);
      return ResponseEntity.ok(ApiResponse.success(PageResponse.of(result)));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest()
          .body(ApiResponse.error(e.getMessage()));
    }
  }

  @GetMapping("/{id}")
  @Operation(summary = "浏览学术成果详情", description = "查看指定成果的详细信息")
  public ResponseEntity<ApiResponse<AchievementDTO>> getAchievementById(
      @Parameter(description = "成果ID") @PathVariable String id) {
    AchievementDTO achievement = achievementService.getById(id);
    // 统计关键词
    if (achievement.getConcepts() != null && !achievement.getConcepts().isEmpty()) {
      paperKeywordService.updateKeywords(achievement.getConcepts());
    }
    return ResponseEntity.ok(ApiResponse.success(achievement));
  }
}
