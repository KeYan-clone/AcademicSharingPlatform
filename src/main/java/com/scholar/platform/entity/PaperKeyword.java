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

  @Column(name = "cnt1")
  private Integer cnt1 = 0;
  @Column(name = "cnt2")
  private Integer cnt2 = 0;
  @Column(name = "cnt3")
  private Integer cnt3 = 0;
  @Column(name = "cnt4")
  private Integer cnt4 = 0;
  @Column(name = "cnt5")
  private Integer cnt5 = 0;
  @Column(name = "cnt6")
  private Integer cnt6 = 0;
  @Column(name = "cnt7")
  private Integer cnt7 = 0;
  @Column(name = "cnt8")
  private Integer cnt8 = 0;
  @Column(name = "cnt9")
  private Integer cnt9 = 0;
  @Column(name = "cnt10")
  private Integer cnt10 = 0;
  @Column(name = "cnt11")
  private Integer cnt11 = 0;
  @Column(name = "cnt12")
  private Integer cnt12 = 0;
}
