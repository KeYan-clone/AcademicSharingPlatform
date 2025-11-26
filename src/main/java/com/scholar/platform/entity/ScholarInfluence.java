package com.scholar.platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "scholar_influence")
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(ScholarInfluence.ScholarInfluenceId.class)
public class ScholarInfluence {

  @Id
  @Column(name = "scholar_id", length = 36)
  private String scholarId;

  @Id
  @Column(name = "year")
  private Integer year;

  @ManyToOne
  @JoinColumn(name = "scholar_id", insertable = false, updatable = false)
  private User scholar;

  @Column(name = "value", nullable = false, precision = 18, scale = 4)
  private BigDecimal value;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ScholarInfluenceId implements Serializable {
    private String scholarId;
    private Integer year;
  }
}
