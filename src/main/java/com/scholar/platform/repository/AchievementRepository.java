package com.scholar.platform.repository;

import com.scholar.platform.entity.Achievement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AchievementRepository extends ElasticsearchRepository<Achievement, String> {

    Optional<Achievement> findByDoi(String doi);

    Page<Achievement> findByTitleContaining(String title, Pageable pageable);

    Page<Achievement> findByConceptsContaining(String concept, Pageable pageable);

    Page<Achievement> findByTitleContainingOrConceptsContaining(String title, String concept, Pageable pageable);

    // 按时间范围检索
    Page<Achievement> findByPublicationDateBetween(String startDate, String endDate, Pageable pageable);

    // 按时间范围和关键词/概念检索（混合搜索标题和概念字段）
    Page<Achievement> findByPublicationDateBetweenAndTitleContainingOrConceptsContaining(
            String startDate, String endDate, String title, String concept, Pageable pageable);

    /**
     * 精确匹配概念字段（使用 match_phrase 进行短语匹配）
     * 可以匹配带空格的完整概念，如 "Computer science"
     */
    @Query("""
      {
        "bool": {
          "must": [
            {
              "match_phrase": {
                "concepts": "?0"
              }
            }
          ]
        }
      }
      """)
    Page<Achievement> findByConceptsExactMatch(String concept, Pageable pageable);

    Page<Achievement> findByInstitutionIds(String institutionId, Pageable pageable);

    Page<Achievement> findByAuthorIds(String authorId, Pageable pageable);

    /**
     * 按时间范围和精确概念检索
     */
    @Query("""
      {
        "bool": {
          "must": [
            {
              "range": {
                "publication_date": {
                  "gte": "?0",
                  "lte": "?1"
                }
              }
            },
            {
              "match_phrase": {
                "concepts": "?2"
              }
            }
          ]
        }
      }
      """)
    Page<Achievement> findByDateRangeAndConceptExactMatch(
            String startDate, String endDate, String concept, Pageable pageable);

    /**
     * 按时间范围和关键词（标题或概念的精确匹配）
     */
    @Query("""
      {
        "bool": {
          "must": [
            {
              "range": {
                "publication_date": {
                  "gte": "?0",
                  "lte": "?1"
                }
              }
            }
          ],
          "should": [
            {
              "match": {
                "title": "?2"
              }
            },
            {
              "match_phrase": {
                "concepts": "?2"
              }
            }
          ],
          "minimum_should_match": 1
        }
      }
      """)
    Page<Achievement> findByDateRangeAndKeywordOrConceptExact(
            String startDate, String endDate, String keyword, Pageable pageable);

    /**
     * 按关键词搜索（支持空格）- 在标题或概念中搜索
     * 使用 match 查询，支持包含空格的关键词如 "artificial intelligence"
     */
    @Query("""
      {
        "bool": {
          "should": [
            {
              "match": {
                "title": {
                  "query": "?0",
                  "operator": "or"
                }
              }
            },
            {
              "match": {
                "concepts": {
                  "query": "?0",
                  "operator": "or"
                }
              }
            }
          ],
          "minimum_should_match": 1
        }
      }
      """)
    Page<Achievement> searchByKeywordWithSpaceSupport(String keyword, Pageable pageable);

    /**
     * 按时间范围和关键词搜索（支持空格）
     * 使用 match 查询，支持包含空格的关键词如 "artificial intelligence"
     */
    @Query("""
      {
        "bool": {
          "must": [
            {
              "range": {
                "publication_date": {
                  "gte": "?0",
                  "lte": "?1"
                }
              }
            }
          ],
          "should": [
            {
              "match": {
                "title": {
                  "query": "?2",
                  "operator": "or"
                }
              }
            },
            {
              "match": {
                "concepts": {
                  "query": "?2",
                  "operator": "or"
                }
              }
            }
          ],
          "minimum_should_match": 1
        }
      }
      """)
    Page<Achievement> searchByDateRangeAndKeywordWithSpaceSupport(
            String startDate, String endDate, String keyword, Pageable pageable);

    List<Achievement> findByStatus(Achievement.AchievementStatus status);
}