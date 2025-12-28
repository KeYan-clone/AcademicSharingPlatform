package com.scholar.platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

  @Id
  @UuidGenerator
  @Column(name = "id", length = 36)
  private String id;

  @Column(name = "username", length = 50, nullable = false)
  private String username;

  @Column(name = "email", length = 100, nullable = false, unique = true)
  private String email;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false)
  private UserRole role = UserRole.USER;

  @Enumerated(EnumType.STRING)
  @Column(name = "certification_status", nullable = false)
  private CertificationStatus certificationStatus = CertificationStatus.NOT_CERTIFIED;

  @Column(name = "preferences", columnDefinition = "TEXT")
  private String preferences;


  public enum UserRole {
    USER, ADMIN
  }

  public enum CertificationStatus {
    NOT_CERTIFIED, PENDING, CERTIFIED
  }
}
