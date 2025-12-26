package com.scholar.platform.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

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
}
