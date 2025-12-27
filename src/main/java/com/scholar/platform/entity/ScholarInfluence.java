package com.scholar.platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "scholar_influence")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScholarInfluence {

    @Id
    @Column(name = "user_id", length = 36)
    private String userId; // 对应 User 表的主键

    @Column(name = "author_name")
    private String authorName;

    @Column(name = "works_count")
    private Integer worksCount;

    @Column(name = "cited_by_cnt")
    private Integer citedByCnt;

    @Column(name = "h_index")
    private Integer hIndex;

    @Column(name = "i10_index")
    private Integer i10Index;

    @Column(name = "domain")
    private String domain;

    @Column(name = "topics", length = 500) // 适当增加长度
    private String topics; // 存储为 "Tag1, Tag2, Tag3"
    
    // 记录关联的 OpenAlex ID，方便后续更新
    @Column(name = "open_alex_id")
    private String openAlexId; 
}