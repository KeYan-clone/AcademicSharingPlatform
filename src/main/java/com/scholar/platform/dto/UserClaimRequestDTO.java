package com.scholar.platform.dto;

import com.scholar.platform.entity.AchievementClaimRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "用户认领请求DTO")
public class UserClaimRequestDTO {
    @Schema(description = "请求ID")
    private String requestId;
    
    @Schema(description = "成果ID")
    private String achievementId;
    
    @Schema(description = "成果标题")
    private String achievementTitle;
    
    @Schema(description = "作者顺序")
    private Integer authorOrder;
    
    @Schema(description = "请求状态")
    private AchievementClaimRequest.ClaimStatus status;
    
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
    
    @Schema(description = "处理消息")
    private String message;
}