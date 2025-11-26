package com.scholar.platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "paper_keywords")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaperKeyword {

  @Id
  @UuidGenerator
  @Column(name = "id", length = 36)
  private String id;

  @Column(name = "keyword", nullable = false)
  private String keyword;

  @Column(name = "cnt", nullable = false)
  private Integer cnt = 0;
}
