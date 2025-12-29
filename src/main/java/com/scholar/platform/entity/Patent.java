package com.scholar.platform.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName = "patent")
public class Patent {

    @Id
    private String id;

    @Field(name = "patentName", type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String patentName;

    @Field(name = "patentType", type = FieldType.Keyword)
    private String patentType;

    @Field(name = "applicant", type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String applicant;

    @Field(name = "applicantType", type = FieldType.Keyword)
    private String applicantType;

    @Field(name = "applicationNumber", type = FieldType.Keyword)
    private String applicationNumber;

    @Field(name = "applicationYear", type = FieldType.Integer)
    private Object applicationYear;

    @Field(name = "grantNumber", type = FieldType.Keyword)
    private String grantNumber;

    @Field(name = "grantYear", type = FieldType.Integer)
    private Object grantYear;

    @Field(name = "ipc", type = FieldType.Keyword)
    private String ipcCode;

    @Field(name = "inventor", type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String inventor;

    @Field(name = "abstractText", type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String abstractText;

    @Field(name = "citedCount", type = FieldType.Integer)
    private Integer citedCount;
}
