package com.scholar.platform.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "openalex_authors")
public class Author {

    @Id
    @Field(type = FieldType.Keyword)
    private String id;

    @Field(name = "display_name", type = FieldType.Text, analyzer = "standard")
    private String displayName;

    @Field(name = "works_count", type = FieldType.Integer)
    private Integer worksCount;

    @Field(name = "cited_by_count", type = FieldType.Integer)
    private Integer citedByCount;

    @Field(name = "summary_stats", type = FieldType.Object)
    private SummaryStats summaryStats;

    @Field(name = "last_known_institutions", type = FieldType.Nested)
    private List<Institution> lastKnownInstitutions;

    @Data
    public static class SummaryStats {
        @Field(name = "h_index", type = FieldType.Integer) 
        private Integer hIndex;

        @Field(name = "i10_index", type = FieldType.Integer) 
        private Integer i10Index;
    }

    @Field(name = "topics", type = FieldType.Nested)
    private List<Topic> topics;

    @Field(name = "fields", type = FieldType.Keyword)
    private List<String> fields;
    
    @Data
    public static class Topic {
        @Field(name = "display_name", type = FieldType.Text)
        private String displayName; // 二级学科名称

        @Field(name = "domain", type = FieldType.Object)
        private Domain domain;      // 所属一级学科
    }

    @Data
    public static class Domain {
        @Field(name = "display_name", type = FieldType.Text)
        private String displayName; // 一级学科名称
    }
}
