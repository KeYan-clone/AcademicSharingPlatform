package com.scholar.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "论坛帖子详情")
public class PostDTO {

  @Schema(description = "帖子ID")
  private String id;

  @Schema(description = "板块ID")
  private String boardId;

  @Schema(description = "板块名称")
  private String boardName;

  @Schema(description = "作者ID")
  private String authorId;

  @Schema(description = "作者用户名")
  private String authorUsername;

  @Schema(description = "标题")
  private String title;

  @Schema(description = "内容")
  private String content;

  @Schema(description = "附件")
  private String attachments;

  @Schema(description = "创建时间")
  private LocalDateTime createdAt;
}
