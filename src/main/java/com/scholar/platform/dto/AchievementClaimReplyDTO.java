package com.scholar.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "成果认领回复DTO")
public class AchievementClaimReplyDTO {
    @Schema(description = "请求ID")
    private String requestId;
    
    @Schema(description = "是否批准")
    private Boolean isApprove;
    
    @Schema(description = "回复消息")
    private String message;
}
