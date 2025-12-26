package com.scholar.platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "achievement_claim_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AchievementClaimRequest {

  @Id
  @UuidGenerator
  @Column(name = "id", length = 36)
  private String id;

  @Column(name = "user_id", nullable = false)
  private String userId;

  @Column(name = "achievement_id", nullable = false)
  private String achievementId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private ClaimStatus status = ClaimStatus.PENDING;

  @Column(name = "message", columnDefinition = "TEXT")
  private String message;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  public enum ClaimStatus {
    PENDING, APPROVED, REJECTED
  }
}
