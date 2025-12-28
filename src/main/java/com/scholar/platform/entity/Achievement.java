package com.scholar.platform.entity;

import com.scholar.platform.dto.AchievementDTO;
import com.scholar.platform.util.IdPrefixUtil;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

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

    @Field(name = "author_names", type = FieldType.Keyword, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private List<String> authorNames;

    @Field(name = "institution_names", type = FieldType.Keyword, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private List<String> institutionNames;

    @Field(type = FieldType.Keyword)
    private AchievementStatus status = AchievementStatus.PENDING;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Authorship {
        @Field(type = FieldType.Nested)
        private Author author;

        @Field(type = FieldType.Nested)
        private List<Institution> institutions;

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

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Institution {
            @Field(type = FieldType.Keyword)
            private String id;

            @Field(name = "display_name", type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
            private String displayName;

            @Field(type = FieldType.Keyword)
            private String ror;

            @Field(type = FieldType.Keyword)
            private String countryCode;

            @Field(type = FieldType.Keyword)
            private String type;
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
        dto.setId(IdPrefixUtil.removeIdPrefix(achievement.getId()));
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
        dto.setAuthorNames(achievement.getAuthorNames());
        dto.setInstitutionNames(achievement.getInstitutionNames());

        if (achievement.getAuthorships() != null) {
            List<AchievementDTO.AuthorInfo> authors = achievement.getAuthorships().stream()
                    .filter(authorship -> authorship.getAuthor() != null)
                    .map(authorship -> new AchievementDTO.AuthorInfo(
                            authorship.getAuthor().getId(),
                            authorship.getAuthor().getDisplayName()
                    ))
                    .collect(Collectors.toList());
            dto.setAuthorships(authors);

            // Extract Institutions
            List<AchievementDTO.InstitutionInfo> institutions = new java.util.ArrayList<>();
            for (Authorship authorship : achievement.getAuthorships()) {
                if (authorship.getInstitutions() != null) {
                    for (Authorship.Institution inst : authorship.getInstitutions()) {
                        if (inst.getId() != null) {
                            institutions.add(new AchievementDTO.InstitutionInfo(inst.getId(), inst.getDisplayName()));
                        }
                    }
                }
            }
            // Remove duplicates based on ID
            List<AchievementDTO.InstitutionInfo> uniqueInstitutions = institutions.stream()
                .collect(Collectors.collectingAndThen(
                    Collectors.toCollection(() -> new java.util.TreeSet<>(java.util.Comparator.comparing(AchievementDTO.InstitutionInfo::getId))),
                    java.util.ArrayList::new));
            
            dto.setInstitution(uniqueInstitutions);
        }

        return dto;
    }


}