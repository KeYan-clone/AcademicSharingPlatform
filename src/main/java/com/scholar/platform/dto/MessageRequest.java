package com.scholar.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "私信发送请求")
public class MessageRequest {

  @NotBlank(message = "接收者ID不能为空")
  @Schema(description = "接收者ID")
  private String recipientId;

  @NotBlank(message = "内容不能为空")
  @Schema(description = "消息内容")
  private String content;
}
