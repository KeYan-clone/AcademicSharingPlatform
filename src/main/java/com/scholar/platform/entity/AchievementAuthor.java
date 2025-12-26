package com.scholar.platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import com.scholar.platform.util.Utils;

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

  @Column(name = "achievement_id", nullable = false, length = 255)
  private String achievementId;

  @Column(name = "author_order", nullable = false)
  private Integer authorOrder;

  @Column(name = "author_name", length = 100, nullable = false)
  private String authorName;

  @ManyToOne
  @JoinColumn(name = "author_user_id")
  private User authorUser;

  public Achievement gAchievement(){
    return Utils.getAchievement(achievementId);
  }
}
