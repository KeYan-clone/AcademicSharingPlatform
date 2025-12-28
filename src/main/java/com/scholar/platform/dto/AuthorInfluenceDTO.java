package com.scholar.platform.dto;

import lombok.Data;

import com.fasterxml.jackson.annotation.JsonProperty;


@Data
public class AuthorInfluenceDTO {
    private Integer worksCount;
    @JsonProperty("citedByCnt")
    private Integer citedByCnt;
    @JsonProperty("hIndex")
    private Integer hIndex;
    @JsonProperty("i10Index")
    private Integer i10Index;
    private String authorName;
    private String domain;       // 一级学科，如 "Computer Science"
    //private String userId;
}