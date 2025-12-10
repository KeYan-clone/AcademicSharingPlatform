package com.scholar.platform.dto;

import com.scholar.platform.entity.Scholar;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "学者信息")
public class ScholarDTO {

  @Schema(description = "用户ID")
  private String userId;

  @Schema(description = "公开姓名")
  private String publicName;

  @Schema(description = "机构")
  private String organization;

  @Schema(description = "职称")
  private String title;

  @Schema(description = "个人简介")
  private String bio;

  @Schema(description = "头像URL")
  private String avatarUrl;

  /**
   * 从Scholar实体创建DTO
   */
  public static ScholarDTO from(Scholar scholar) {
    ScholarDTO dto = new ScholarDTO();
    dto.setUserId(scholar.getUserId());
    dto.setPublicName(scholar.getPublicName());
    dto.setOrganization(scholar.getOrganization());
    dto.setTitle(scholar.getTitle());
    dto.setBio(scholar.getBio());
    dto.setAvatarUrl(scholar.getAvatarUrl());
    return dto;
  }
}
