package com.scholar.platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "forum_boards")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForumBoard {

  @Id
  //@UuidGenerator
  @Column(name = "id", length = 36)
  private String id;

  @Column(name = "name", length = 100, nullable = false, unique = true)
  private String name;

  @Column(name = "type", nullable = false)
  private Integer type;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;
}
