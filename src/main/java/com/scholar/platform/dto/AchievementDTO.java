package com.scholar.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "成果信息")
public class AchievementDTO {

  @Schema(description = "成果ID")
  private String id;

  @Schema(description = "DOI")
  private String doi;

  @Schema(description = "标题")
  private String title;

  @Schema(description = "作者列表")
  private List<AuthorInfo> authorships;

  @Schema(description = "机构信息列表")
  private List<InstitutionInfo> institution;

  @Schema(description = "发表日期")
  private String publicationDate;

  @Schema(description = "相关作品")
  private List<String> relatedWorks;

  @Schema(description = "引用次数")
  private Integer citedByCount;

  @Schema(description = "语言")
  private String language;

  @Schema(description = "学科概念")
  private List<String> concepts;

  @Schema(description = "论文链接")
  private String landingPageUrl;

  @Schema(description = "摘要")
  private String abstractText;

  @Schema(description = "收藏次数")
  private Integer favouriteCount;

  @Schema(description = "阅读次数")
  private Integer readCount;

  @Schema(description = "作者ID列表")
  private List<String> authorIds;

  @Schema(description = "机构ID列表")
  private List<String> institutionIds;

  @Schema(description = "作者姓名列表")
  private List<String> authorNames;

  @Schema(description = "机构名称列表")
  private List<String> institutionNames;
  
  @Schema(description = "是否已收藏")
  private Boolean isFavourite;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "作者信息")
  public static class AuthorInfo {
    @Schema(description = "作者ID")
    private String id;

    @Schema(description = "作者姓名")
    private String name;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "机构信息")
  public static class InstitutionInfo {
    @Schema(description = "机构ID")
    private String id;

    @Schema(description = "机构名称")
    private String displayName;
  }
}
