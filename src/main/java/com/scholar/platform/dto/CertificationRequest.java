package com.scholar.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "学者认证申请请求")
public class CertificationRequest {

  @NotBlank(message = "真实姓名不能为空")
  @Schema(description = "真实姓名")
  private String realName;

  @NotBlank(message = "所属机构不能为空")
  @Schema(description = "所属机构")
  private String organization;

  @Schema(description = "机构邮箱")
  private String orgEmail;

  @Schema(description = "职称/学位")
  private String title;

  @Schema(description = "证明材料(JSON数组)")
  private String proofMaterials;
}
