package com.scholar.platform.dto;

import lombok.Data;
import java.util.List;

@Data
public class AuthorInfluenceDTO {
    private Integer worksCount;
    private Integer citedByCnt;
    private Integer hIndex;
    private Integer i10Index;
    private String authorName;
    private String domain;       // 一级学科，如 "Computer Science"
    private List<String> topics; // 研究方向/二级学科，如 ["Artificial Intelligence", "Machine Learning"]
}