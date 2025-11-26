package com.scholar.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "论坛帖子创建请求")
public class PostCreateRequest {

  @NotBlank(message = "板块ID不能为空")
  @Schema(description = "板块ID")
  private String boardId;

  @NotBlank(message = "标题不能为空")
  @Schema(description = "标题")
  private String title;

  @NotBlank(message = "内容不能为空")
  @Schema(description = "内容")
  private String content;

  @Schema(description = "附件列表(JSON)")
  private String attachments;
}
