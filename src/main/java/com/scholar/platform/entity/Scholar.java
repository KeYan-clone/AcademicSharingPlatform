package com.scholar.platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "scholars")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Scholar {

  @Id
  @Column(name = "user_id", length = 36)
  private String userId;
//
//  @OneToOne
//  @MapsId
//  @JoinColumn(name = "user_id")
//  private User user;

  @Column(name = "public_name", length = 100, nullable = false)
  private String publicName;

  @Column(name = "organization")
  private String organization;

  @Column(name = "title", length = 100)
  private String title;

  @Column(name = "bio", columnDefinition = "TEXT")
  private String bio;

  @Column(name = "avatar_url")
  private String avatarUrl;
}
