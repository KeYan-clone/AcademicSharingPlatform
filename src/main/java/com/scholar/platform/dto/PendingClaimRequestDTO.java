package com.scholar.platform.dto;

import com.scholar.platform.entity.AchievementClaimRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "待审核认领请求DTO")
public class PendingClaimRequestDTO {
    @Schema(description = "请求ID")
    private String requestId;
    
    @Schema(description = "用户ID")
    private String userId;
    
    @Schema(description = "用户名")
    private String username;
    
    @Schema(description = "成果ID")
    private String achievementId;
    
    @Schema(description = "成果标题")
    private String achievementTitle;
    
    @Schema(description = "作者顺序")
    private Integer authorOrder;
    
    @Schema(description = "请求消息")
    private String message;
    
    @Schema(description = "请求状态")
    private AchievementClaimRequest.ClaimStatus status;
    
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}