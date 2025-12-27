package com.scholar.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthorRelationDTO {
    private String author1Id;
    private String author1Name;
    private String author2Id;
    private String author2Name;
    private Integer count;
}
