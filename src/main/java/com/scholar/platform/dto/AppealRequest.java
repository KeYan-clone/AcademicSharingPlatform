package com.scholar.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "用户申诉请求")
public class AppealRequest {

  @NotNull(message = "申诉类型不能为空")
  @Schema(description = "申诉类型", example = "identity_stolen")
  private String appealType;

  @NotBlank(message = "目标ID不能为空")
  @Schema(description = "被申诉的学者或成果ID")
  private String targetId;

  @NotBlank(message = "申诉原因不能为空")
  @Schema(description = "申诉原因")
  private String reason;

  @Schema(description = "证据材料(JSON数组字符串)")
  private String evidenceMaterials;
}
