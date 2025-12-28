package com.scholar.platform.dto;

import com.scholar.platform.entity.KnowledgeDocument;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDocumentResponse {
  private String id;
  private String knowledgeBaseId;
  private String originalFilename;
  private KnowledgeDocument.DocumentStatus status;
  private Long fileSize;
  private Integer pageCount;
  private String summary;
  private LocalDateTime parsedAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private String parseError;
}
