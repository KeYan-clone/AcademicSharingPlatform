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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
            @Parameter(description = "学者姓名关键词") @RequestParam(required = false) Optional<String> name,
            @Parameter(description = "机构") @RequestParam(required = false) Optional<String> organization,
            @Parameter(description = "研究领域") @RequestParam(required = false) Optional<String> field
    ) {
        List<ScholarDTO> scholars = scholarService.searchScholar(name, organization, field);
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

    //todo：加入学者合作关系网络，修改返回的数据结构
    @GetMapping("/{userId}/collaboration-network")
    @Operation(summary = "查看学者合作关系网络", description = "获取指定学者的合作关系网络图数据")
    public ResponseEntity<ApiResponse<List<ScholarDTO>>> getCollaborationNetwork(
            @Parameter(description = "学者用户ID") @PathVariable String userId
    ) {
        ArrayList<ScholarDTO> collaborators = (ArrayList<ScholarDTO>) scholarService.findAllCollaborators(userId);

        return ResponseEntity.ok(ApiResponse.success(collaborators));
    }
}
