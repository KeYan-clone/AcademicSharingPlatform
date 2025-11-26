package com.scholar.platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "achievement_authors")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AchievementAuthor {

  @Id
  @UuidGenerator
  @Column(name = "id", length = 36)
  private String id;

  @ManyToOne
  @JoinColumn(name = "achievement_id", nullable = false)
  private Achievement achievement;

  @Column(name = "author_order", nullable = false)
  private Integer authorOrder;

  @Column(name = "author_name", length = 100, nullable = false)
  private String authorName;

  @ManyToOne
  @JoinColumn(name = "author_user_id")
  private User authorUser;
}
