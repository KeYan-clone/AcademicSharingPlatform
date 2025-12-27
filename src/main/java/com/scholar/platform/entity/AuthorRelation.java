package com.scholar.platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "author_relation")
@IdClass(AuthorRelationId.class)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthorRelation {

    @Id
    @Column(name = "author1_id", length = 100)
    private String author1Id;

    @Id
    @Column(name = "author2_id", length = 100)
    private String author2Id;

    @Column(name = "author1_name")
    private String author1Name;

    @Column(name = "author2_name")
    private String author2Name;

    @Column(name = "`count`")
    private Integer count;
}
