package com.scholar.platform.entity;

import com.scholar.platform.dto.AchievementDTO;
import jakarta.persistence.PrePersist;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Getter
@Setter
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

    @Field(type = FieldType.Keyword)
    private AchievementStatus status = AchievementStatus.PENDING;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Authorship {
        @Field(type = FieldType.Nested)
        private Author author;

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


    public enum AchievementStatus {
        PENDING, APPROVED, REJECTED
    }

    /**
     * 转换为DTO，提取作者信息
     */
    public  static AchievementDTO toDTO(Achievement achievement) {
        AchievementDTO dto = new AchievementDTO();
        dto.setId(achievement.getId());
        dto.setDoi(achievement.getDoi());
        dto.setTitle(achievement.getTitle());
        dto.setPublicationDate(achievement.getPublicationDate());
        dto.setRelatedWorks(achievement.getRelatedWorks());
        dto.setCitedByCount(achievement.getCitedByCount());
        dto.setLanguage(achievement.getLanguage());
        dto.setConcepts(achievement.getConcepts());
        dto.setLandingPageUrl(achievement.getLandingPageUrl());
        dto.setAbstractText(achievement.getAbstractText());
        dto.setFavouriteCount(achievement.getFavouriteCount());
        dto.setReadCount(achievement.getReadCount());
        dto.setAuthorIds(achievement.getAuthorIds());
        dto.setInstitutionIds(achievement.getInstitutionIds());

        if (achievement.getAuthorships() != null) {
            List<AchievementDTO.AuthorInfo> authors = achievement.getAuthorships().stream()
                    .filter(authorship -> authorship.getAuthor() != null)
                    .map(authorship -> new AchievementDTO.AuthorInfo(
                            authorship.getAuthor().getId(),
                            authorship.getAuthor().getDisplayName()
                    ))
                    .collect(Collectors.toList());
            dto.setAuthorships(authors);
        }

        return dto;
    }


}