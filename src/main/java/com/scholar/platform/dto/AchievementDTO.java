package com.scholar.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "成果信息")
public class AchievementDTO {

  @Schema(description = "成果ID")
  private String id;

  @Schema(description = "成果类型")
  private String type;

  @Schema(description = "标题")
  private String title;

  @Schema(description = "发表年份")
  private Integer publicationYear;

  @Schema(description = "摘要")
  private String abstractText;

  @Schema(description = "DOI")
  private String doi;

  @Schema(description = "发表期刊/会议")
  private String publicationVenue;

  @Schema(description = "引用次数")
  private Integer citationCount;

  @Schema(description = "创建时间")
  private LocalDateTime createdAt;
}
