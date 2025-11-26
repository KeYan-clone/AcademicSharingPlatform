package com.scholar.platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "scholar_certifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScholarCertification {

  @Id
  @UuidGenerator
  @Column(name = "id", length = 36)
  private String id;

  @ManyToOne
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "real_name", length = 100, nullable = false)
  private String realName;

  @Column(name = "organization", nullable = false)
  private String organization;

  @Column(name = "org_email", length = 100)
  private String orgEmail;

  @Column(name = "title", length = 100)
  private String title;

  @Column(name = "proof_materials", columnDefinition = "TEXT")
  private String proofMaterials;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private CertificationStatus status = CertificationStatus.PENDING;

  @Column(name = "rejection_reason", columnDefinition = "TEXT")
  private String rejectionReason;

  @Column(name = "submitted_at", nullable = false, updatable = false)
  private LocalDateTime submittedAt;

  @ManyToOne
  @JoinColumn(name = "processed_by_admin_id")
  private User processedByAdmin;

  @Column(name = "processed_at")
  private LocalDateTime processedAt;

  @PrePersist
  protected void onCreate() {
    submittedAt = LocalDateTime.now();
  }

  public enum CertificationStatus {
    PENDING, APPROVED, REJECTED
  }
}
