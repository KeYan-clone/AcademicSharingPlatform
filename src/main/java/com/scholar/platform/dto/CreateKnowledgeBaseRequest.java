package com.scholar.platform.dto;

import com.scholar.platform.entity.KnowledgeBase;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateKnowledgeBaseRequest {

  @NotBlank(message = "知识库名称不能为空")
  @Size(max = 100, message = "名称长度不能超过100字符")
  private String name;

  @Size(max = 2000, message = "描述长度不能超过2000字符")
  private String description;

  private KnowledgeBase.Visibility visibility = KnowledgeBase.Visibility.PRIVATE;
}
