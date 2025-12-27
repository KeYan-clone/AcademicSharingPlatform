package com.scholar.platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "scholar_ranking")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScholarRanking {

    @Id
    @Column(length = 100)
    private String id;

    @Column(name = "display_name")
    private String displayName;

    @Column(length = 100)
    private String domain;

    @Column(name = "primary_tags")
    private String primaryTags;

    @Column(name = "h_index")
    private Integer hIndex;

    @Column(name = "i10_index")
    private Integer i10Index;

    @Column(name = "works_count")
    private Integer worksCount;

    @Column(name = "influence_score")
    private Double influenceScore;

    @Column(name = "rank_in_domain")
    private Integer rankInDomain;
}