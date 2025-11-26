package com.scholar.platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "achievements")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Achievement {

  @Id
  @UuidGenerator
  @Column(name = "id", length = 36)
  private String id;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false)
  private AchievementType type;

  @Column(name = "title", columnDefinition = "TEXT", nullable = false)
  private String title;

  @Column(name = "publication_year")
  private Integer publicationYear;

  @Column(name = "abstract", columnDefinition = "TEXT")
  private String abstractText;

  @Column(name = "doi", length = 100, unique = true)
  private String doi;

  @Column(name = "publication_venue")
  private String publicationVenue;

  @Column(name = "citation_count", nullable = false)
  private Integer citationCount = 0;

  @Column(name = "source_data", columnDefinition = "JSON")
  private String sourceData;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }

  public enum AchievementType {
    PAPER, PATENT, PROJECT, AWARD
  }
}
