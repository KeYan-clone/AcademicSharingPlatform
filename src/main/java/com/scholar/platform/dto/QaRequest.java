package com.scholar.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class QaRequest {
  @NotBlank(message = "问题不能为空")
  private String question;

  @Positive(message = "topK 必须大于0")
  private Integer topK = 5;

  private String kbId;
}
