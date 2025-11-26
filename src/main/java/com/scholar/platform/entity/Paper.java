package com.scholar.platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "papers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Paper {

  @Id
  @Column(name = "id", length = 100)
  private String id;

  @Column(name = "title", length = 100)
  private String title;

  @Column(name = "display_name", length = 100)
  private String displayName;

  @Column(name = "publication_year")
  private Integer publicationYear;

  @Column(name = "publication_date")
  private LocalDateTime publicationDate;

  @Column(name = "type", length = 100)
  private String type;

  @Column(name = "doi", length = 100)
  private String doi;

  @Column(name = "host_venue", columnDefinition = "JSON")
  private String hostVenue;

  @Column(name = "authorships", columnDefinition = "JSON")
  private String authorships;

  @Column(name = "abstract_text", columnDefinition = "TEXT")
  private String abstractText;

  @Column(name = "concepts", columnDefinition = "JSON")
  private String concepts;

  @Column(name = "cited_by_count")
  private Integer citedByCount;
}
