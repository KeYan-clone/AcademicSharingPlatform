package com.scholar.platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "follows")
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(Follow.FollowId.class)
public class Follow {

  @Id
  @Column(name = "follower_id", length = 36)
  private String followerId;

  @Id
  @Column(name = "following_id", length = 36)
  private String followingId;

  @ManyToOne
  @JoinColumn(name = "follower_id", insertable = false, updatable = false)
  private User follower;

  @ManyToOne
  @JoinColumn(name = "following_id", insertable = false, updatable = false)
  private User following;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class FollowId implements Serializable {
    private String followerId;
    private String followingId;
  }
}
