package com.scholar.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "回复创建请求")
public class ReplyCreateRequest {

  @NotBlank(message = "帖子ID不能为空")
  @Schema(description = "帖子ID")
  private String postId;

  @NotBlank(message = "内容不能为空")
  @Schema(description = "内容")
  private String content;
}
