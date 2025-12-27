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

    Page<Achievement> findByPublicationDateBetween(String startDate, String endDate, Pageable pageable);

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
     * 双关键词搜索（支持中英文混合检索）
     * 使用 function_score 查询应用权重算法
     */
    @Query("""
      {
        "function_score": {
          "query": {
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
                },
                {
                  "match": {
                    "title": {
                      "query": "?1",
                      "operator": "or"
                    }
                  }
                },
                {
                  "match": {
                    "concepts": {
                      "query": "?1",
                      "operator": "or"
                    }
                  }
                }
              ],
              "minimum_should_match": 1
            }
          },
          "functions": [
            {
              "field_value_factor": {
                "field": "cited_by_count",
                "factor": 1.2,
                "modifier": "log1p",
                "missing": 0
              }
            },
            {
              "field_value_factor": {
                "field": "favourite_count",
                "factor": 1.0,
                "modifier": "log1p",
                "missing": 0
              }
            },
            {
              "field_value_factor": {
                "field": "read_count",
                "factor": 0.8,
                "modifier": "log1p",
                "missing": 0
              }
            }
          ],
          "score_mode": "sum",
          "boost_mode": "multiply",
          "max_boost": 42
        }
      }
      """)
    Page<Achievement> searchByTwoKeywords(String keyword1, String keyword2, Pageable pageable);

    /**
     * 按关键词搜索（支持空格）- 在标题或概念中搜索
     * 使用 function_score 查询应用权重算法
     */
    @Query("""
      {
        "function_score": {
          "query": {
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
          },
          "functions": [
            {
              "field_value_factor": {
                "field": "cited_by_count",
                "factor": 1.2,
                "modifier": "log1p",
                "missing": 0
              }
            },
            {
              "field_value_factor": {
                "field": "favourite_count",
                "factor": 1.0,
                "modifier": "log1p",
                "missing": 0
              }
            },
            {
              "field_value_factor": {
                "field": "read_count",
                "factor": 0.8,
                "modifier": "log1p",
                "missing": 0
              }
            }
          ],
          "score_mode": "sum",
          "boost_mode": "multiply",
          "max_boost": 42
        }
      }
      """)
    Page<Achievement> searchByKeywordWithSpaceSupport(String keyword, Pageable pageable);

    /**
     * 按时间范围和关键词搜索（支持空格）
     * 使用 function_score 查询应用权重算法
     */
    @Query("""
      {
        "function_score": {
          "query": {
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
          },
          "functions": [
            {
              "field_value_factor": {
                "field": "cited_by_count",
                "factor": 1.2,
                "modifier": "log1p",
                "missing": 0
              }
            },
            {
              "field_value_factor": {
                "field": "favourite_count",
                "factor": 1.0,
                "modifier": "log1p",
                "missing": 0
              }
            },
            {
              "field_value_factor": {
                "field": "read_count",
                "factor": 0.8,
                "modifier": "log1p",
                "missing": 0
              }
            }
          ],
          "score_mode": "sum",
          "boost_mode": "multiply",
          "max_boost": 42
        }
      }
      """)
    Page<Achievement> searchByDateRangeAndKeywordWithSpaceSupport(
            String startDate, String endDate, String keyword, Pageable pageable);

    List<Achievement> findByStatus(Achievement.AchievementStatus status);

    /**
     * 高级检索组合查询（带权重排序）
     * 支持关键词、概念、时间范围、作者和机构的组合检索
     * 使用 function_score 查询应用权重算法
     * 权重因子 = (log(1+cited_by_count)×1.2 + log(1+favourite_count)×1.0 + log(1+read_count)×0.8) / 3
     */
    @Query("""
      {
        "function_score": {
          "query": {
            "bool": {
              "must": [
                {
                  "bool": {
                    "should": [
                      { "range": { "publication_date": { "gte": "?0", "lte": "?1" } } }
                    ],
                    "minimum_should_match": 0
                  }
                },
                {
                  "bool": {
                    "should": [
                      { "term": { "institution_ids": "?2" } }
                    ],
                    "minimum_should_match": 0
                  }
                },
                {
                  "bool": {
                    "should": [
                      { "term": { "author_ids": "?3" } }
                    ],
                    "minimum_should_match": 0
                  }
                },
                {
                  "bool": {
                    "should": [
                      { "match_phrase": { "concepts": "?4" } }
                    ],
                    "minimum_should_match": 0
                  }
                }
              ],
              "should": [
                {
                  "match": {
                    "title": {
                      "query": "?5",
                      "operator": "or"
                    }
                  }
                },
                {
                  "match": {
                    "concepts": {
                      "query": "?5",
                      "operator": "or"
                    }
                  }
                }
              ],
              "minimum_should_match": 1
            }
          },
          "functions": [
            {
              "field_value_factor": {
                "field": "cited_by_count",
                "factor": 1.2,
                "modifier": "log1p",
                "missing": 0
              }
            },
            {
              "field_value_factor": {
                "field": "favourite_count",
                "factor": 1.0,
                "modifier": "log1p",
                "missing": 0
              }
            },
            {
              "field_value_factor": {
                "field": "read_count",
                "factor": 0.8,
                "modifier": "log1p",
                "missing": 0
              }
            }
          ],
          "score_mode": "sum",
          "boost_mode": "multiply",
          "max_boost": 42
        }
      }
      """)
    Page<Achievement> advancedSearchWithWeighting(
            String startDate, String endDate,
            String institutionId, String authorId,
            String concept, String keyword,
            Pageable pageable);
}