package com.scholar.platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_collections")
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(UserCollection.UserCollectionId.class)
public class UserCollection {

  @Id
  @Column(name = "user_id", length = 36)
  private String userId;

  @Id
  @Column(name = "achievement_id", length = 36)
  private String achievementId;

  @ManyToOne
  @JoinColumn(name = "user_id", insertable = false, updatable = false)
  private User user;


  @Column(name = "saved_at", nullable = false, updatable = false)
  private LocalDateTime savedAt;

  @PrePersist
  protected void onCreate() {
    savedAt = LocalDateTime.now();
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UserCollectionId implements Serializable {
    private String userId;
    private String achievementId;
  }
}
