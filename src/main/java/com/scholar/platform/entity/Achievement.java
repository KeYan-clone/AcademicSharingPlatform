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
@Document(indexName = "openalex_works")
public class Achievement {

  @Id
  @Field(type = FieldType.Keyword)
  private String id;

  @Field(type = FieldType.Keyword)
  private String doi;

  @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
  private String title;

  @Field(type = FieldType.Nested)
  private List<Authorship> authorships;

  @Field(name = "publication_date", type = FieldType.Date, pattern = "yyyy-MM-dd")
  private String publicationDate;

  @Field(name = "has_abstract", type = FieldType.Boolean)
  private Boolean hasAbstract;

  @Field(name = "related_works", type = FieldType.Keyword)
  private List<String> relatedWorks;

  @Field(name = "cited_by_count", type = FieldType.Integer)
  private Integer citedByCount;

  @Field(type = FieldType.Keyword)
  private String language;

  @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
  private List<String> concepts;

  @Field(name = "landing_page_url", type = FieldType.Keyword)
  private String landingPageUrl;

  @Field(name = "abstract", type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
  private String abstractText;

  @Field(name = "authors_count", type = FieldType.Integer)
  private Integer authorsCount;

  @Field(name = "favouriteCount", type = FieldType.Integer)
  private Integer favouriteCount;

  @Field(name = "readCount", type = FieldType.Integer)
  private Integer readCount;

  @Field(name = "author_ids", type = FieldType.Keyword)
  private List<String> authorIds;

  @Field(name = "institution_ids", type = FieldType.Keyword)
  private List<String> institutionIds;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Authorship {
    @Field(type = FieldType.Nested)
    private Author author;

    @Field(name = "author_position", type = FieldType.Keyword)
    private String authorPosition;

    @Field(name = "is_corresponding", type = FieldType.Boolean)
    private Boolean isCorresponding;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Author {
    @Field(type = FieldType.Keyword)
    private String id;

    @Field(name = "display_name", type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String displayName;

    @Field(type = FieldType.Keyword)
    private String orcid;
  }
}
