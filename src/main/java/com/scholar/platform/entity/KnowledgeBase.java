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
@Table(name = "knowledge_bases")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBase {

  @Id
  @UuidGenerator
  @Column(name = "id", length = 36)
  private String id;

  @Column(name = "user_id", length = 36, nullable = false)
  private String userId;

  @Column(name = "name", length = 100, nullable = false)
  private String name;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "storage_path", length = 512, nullable = false)
  private String storagePath;

  @Enumerated(EnumType.STRING)
  @Column(name = "visibility", length = 20, nullable = false)
  private Visibility visibility = Visibility.PRIVATE;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public enum Visibility {
    PRIVATE,
    PUBLIC
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
