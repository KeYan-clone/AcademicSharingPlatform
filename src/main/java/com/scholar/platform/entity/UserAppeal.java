package com.scholar.platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_appeals")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAppeal {

  @Id
  @UuidGenerator
  @Column(name = "id", length = 36)
  private String id;

  @ManyToOne
  @JoinColumn(name = "applicant_user_id", nullable = false)
  private User applicant;

  @Enumerated(EnumType.STRING)
  @Column(name = "appeal_type", nullable = false)
  private AppealType appealType;

  @Column(name = "target_id", nullable = false)
  private String targetId;

  @Column(name = "reason", columnDefinition = "TEXT", nullable = false)
  private String reason;

  @Column(name = "evidence_materials", columnDefinition = "TEXT")
  private String evidenceMaterials;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private AppealStatus status = AppealStatus.PENDING;

  @ManyToOne
  @JoinColumn(name = "processed_by_admin_id")
  private User processedByAdmin;

  @Column(name = "processed_at")
  private LocalDateTime processedAt;

  public enum AppealType {
    IDENTITY_STOLEN, ACHIEVEMENT_STOLEN
  }

  public enum AppealStatus {
    PENDING, APPROVED, REJECTED
  }
}
