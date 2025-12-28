package com.scholar.platform.dto;

import lombok.Data;
import java.util.List;
import java.util.Arrays;

@Data
public class ScholarRankingDTO {
    private Integer rank;
    private ScholarInfo scholar;
    private Double influenceScore;

    @Data
    public static class ScholarInfo {
        private String id;
        private String displayName;
        private List<String> primaryTags; // 将逗号分隔的字符串转为列表
        private Integer hIndex;
        private Integer i10Index;
        private Integer worksCount;
        private Integer citedCount;
    }
    
    // 静态工厂方法：将实体转换为DTO
    public static ScholarRankingDTO fromEntity(com.scholar.platform.entity.ScholarRanking entity) {
        ScholarRankingDTO dto = new ScholarRankingDTO();
        dto.setInfluenceScore(entity.getInfluenceScore());
        
        ScholarInfo info = new ScholarInfo();
        info.setId(entity.getId());
        info.setDisplayName(entity.getDisplayName());
        info.setHIndex(entity.getHIndex());
        info.setI10Index(entity.getI10Index());
        info.setWorksCount(entity.getWorksCount());
        
        // 处理 tags: "Tag1, Tag2" -> ["Tag1", "Tag2"]
        if (entity.getPrimaryTags() != null && !entity.getPrimaryTags().isEmpty()) {
            info.setPrimaryTags(Arrays.asList(entity.getPrimaryTags().split(",\\s*")));
        }
        
        dto.setScholar(info);
        return dto;
    }
}