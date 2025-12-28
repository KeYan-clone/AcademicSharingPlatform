package com.scholar.platform.dto;

import com.scholar.platform.entity.KnowledgeBase;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBaseResponse {
  private String id;
  private String name;
  private String description;
  private KnowledgeBase.Visibility visibility;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
