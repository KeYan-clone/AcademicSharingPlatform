package com.scholar.platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "knowledge_documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDocument {

  @Id
  @UuidGenerator
  @Column(name = "id", length = 36)
  private String id;

  @Column(name = "knowledge_base_id", length = 36, nullable = false)
  private String knowledgeBaseId;

  @Column(name = "user_id", length = 36, nullable = false)
  private String userId;

  @Column(name = "original_filename", length = 255, nullable = false)
  private String originalFilename;

  @Column(name = "stored_filename", length = 255, nullable = false)
  private String storedFilename;

  @Column(name = "storage_path", length = 512, nullable = false)
  private String storagePath;

  @Column(name = "text_path", length = 512)
  private String textPath;

  @Column(name = "content_type", length = 100)
  private String contentType;

  @Column(name = "file_size")
  private Long fileSize;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 30, nullable = false)
  private DocumentStatus status = DocumentStatus.PENDING;

  @Column(name = "page_count")
  private Integer pageCount;

  @Column(name = "summary", columnDefinition = "TEXT")
  private String summary;

  @Column(name = "parse_error", columnDefinition = "TEXT")
  private String parseError;

  @Column(name = "parsed_at")
  private LocalDateTime parsedAt;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public enum DocumentStatus {
    PENDING,
    PARSING,
    READY,
    FAILED
  }

  @PrePersist
  public void prePersist() {
    this.createdAt = LocalDateTime.now();
    this.updatedAt = this.createdAt;
  }

  @PreUpdate
  public void preUpdate() {
    this.updatedAt = LocalDateTime.now();
  }
}
