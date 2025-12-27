package com.scholar.platform.service;

import com.scholar.platform.dto.AchievementDTO;
import com.scholar.platform.entity.*;
import com.scholar.platform.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.ScriptType;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.scholar.platform.util.IdPrefixUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final InstitutionRepository institutionRepository;
    private final AuthorRepository authorRepository;
    // private final ConceptRepository conceptRepository;
    private final PaperKeywordRepository paperKeywordRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final UserRepository userRepository;
    private final UserCollectionRepository userCollectionRepository;
    private final TranslationService translationService;

    /**
     * 通过关键词搜索（带加权排序）
     * 使用 function_score 查询应用权重算法
     * 权重因子 = (log(1+cited_by_count)×1.2 + log(1+favourite_count)×1.0 + log(1+read_count)×0.8) / 3
     */
    public Page<AchievementDTO> searchByKeyword(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("请输入检索内容");
        }
        return achievementRepository.searchByKeywordWithWeighting(keyword, pageable)
                .map(this::toDTO);
    }

    /**
     * 按照concepts精确匹配检索（支持带空格的完整短语，如 "Computer science"）
     * 使用 match_phrase 确保精确匹配，不会匹配到包含该词的其他概念
     */
    public Page<AchievementDTO> searchByConceptsExact(String concept, Pageable pageable) {
        if (concept == null || concept.trim().isEmpty()) {
            throw new IllegalArgumentException("请输入检索内容");
        }
        return achievementRepository.findByConceptsExactMatch(concept, pageable)
                .map(this::toDTO);
    }

    /**
     * 按时间范围检索（带加权排序）
     */
    public Page<AchievementDTO> searchByDateRange(String startDate, String endDate, Pageable pageable) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("起止时间不能为空");
        }
        // 由于时间范围检索没有关键词，使用非加权版本
        return achievementRepository.findByPublicationDateBetween(startDate, endDate, pageable)
                .map(this::toDTO);
    }

    /**
     * 高级检索：支持关键词、概念、时间范围、作者和机构的组合检索
     * 现在统一使用加权搜索作为默认功能
     *
     * @param keyword         关键词（模糊搜索标题和概念，带加权）
     * @param field           学科领域/概念（精确匹配，由用户从下拉列表选择）
     * @param startDate       开始日期
     * @param endDate         结束日期
     * @param authorName      作者姓名（精确匹配）
     * @param institutionName 机构名称（精确匹配）
     * @param pageable        分页参数
     */
    public Page<AchievementDTO> advancedSearch(String keyword, String field,
                                               String startDate, String endDate,
                                               String authorName, String institutionName,
                                               Pageable pageable) {
        // 1. 预先解析 ID (暂缓搜索)
        String institutionId = null;
        if (institutionName != null && !institutionName.trim().isEmpty()) {
            Page<Institution> institutions = institutionRepository.findByDisplayName(institutionName, Pageable.ofSize(1));
            if (institutions.hasContent()) {
                institutionId = institutions.getContent().get(0).getId();
            } else {
                return Page.empty(pageable);
            }
        }

        String authorId = null;
        if (authorName != null && !authorName.trim().isEmpty()) {
            Page<Author> authors = authorRepository.findByDisplayName(authorName, Pageable.ofSize(1));
            if (authors.hasContent()) {
                authorId = authors.getContent().get(0).getId();
            } else {
                return Page.empty(pageable);
            }
        }

        // 2. 如果有关键词和时间范围，优先使用带权重的日期范围+关键词搜索
        if (keyword != null && !keyword.trim().isEmpty() && startDate != null && endDate != null) {
            Page<Achievement> results = achievementRepository.searchByDateRangeAndKeywordWithWeighting(
                    startDate, endDate, keyword, pageable);
            
            // 3. 再进行内存中的过滤（概念、作者、机构）
            if (field != null && !field.trim().isEmpty() ||
                authorId != null || institutionId != null) {
                return filterResults(results, field, authorId, institutionId, pageable);
            }
            return results.map(this::toDTO);
        }

        // 2b. 如果只有关键词，使用加权关键词搜索
        if (keyword != null && !keyword.trim().isEmpty()) {
            Page<Achievement> results = achievementRepository.searchByKeywordWithWeighting(keyword, pageable);
            
            // 进行内存中的过滤（时间范围、概念、作者、机构）
            if (startDate != null && endDate != null ||
                field != null && !field.trim().isEmpty() ||
                authorId != null || institutionId != null) {
                return filterResults(results, field, authorId, institutionId, pageable, startDate, endDate);
            }
            return results.map(this::toDTO);
        }

        // 2c. 没有关键词，使用 Criteria 进行其他维度的组合搜索
        Criteria criteria = new Criteria();
        boolean hasCondition = false;

        if (institutionId != null) {
            criteria = criteria.and("institution_ids").is(institutionId);
            hasCondition = true;
        }

        if (authorId != null) {
            criteria = criteria.and("author_ids").is(authorId);
            hasCondition = true;
        }

        if (startDate != null && endDate != null) {
            criteria = criteria.and("publication_date").between(startDate, endDate);
            hasCondition = true;
        }

        if (field != null && !field.trim().isEmpty()) {
            criteria = criteria.and("concepts").matches(field);
            hasCondition = true;
        }

        if (!hasCondition) {
            // 尝试获取最热的 concept
            PaperKeyword topKeyword = paperKeywordRepository.findFirstByOrderByCntDesc();
            if (topKeyword != null && topKeyword.getKeyword() != null) {
                // 使用加权搜索显示最热的关键词
                return achievementRepository.searchByKeywordWithWeighting(topKeyword.getKeyword(), pageable)
                        .map(this::toDTO);
            } else {
                throw new IllegalArgumentException("请输入检索内容");
            }
        }

        CriteriaQuery query = new CriteriaQuery(criteria);
        query.setPageable(pageable);

        SearchHits<Achievement> searchHits = elasticsearchOperations.search(query, Achievement.class);

        List<AchievementDTO> list = searchHits.getSearchHits().stream()
                .map(hit -> toDTO(hit.getContent()))
                .collect(Collectors.toList());

        return new PageImpl<>(list, pageable, searchHits.getTotalHits());
    }

    /**
     * 辅助方法：在内存中过滤结果
     */
    private Page<AchievementDTO> filterResults(Page<Achievement> results, String field,
                                               String authorId, String institutionId,
                                               Pageable pageable) {
        return filterResults(results, field, authorId, institutionId, pageable, null, null);
    }

    /**
     * 辅助方法：在内存中过滤结果（包含日期范围）
     */
    private Page<AchievementDTO> filterResults(Page<Achievement> results, String field,
                                               String authorId, String institutionId,
                                               Pageable pageable, String startDate, String endDate) {
        List<AchievementDTO> filtered = results.getContent().stream()
                .filter(achievement -> {
                    // 按概念过滤
                    if (field != null && !field.trim().isEmpty()) {
                        if (achievement.getConcepts() == null ||
                            !achievement.getConcepts().contains(field)) {
                            return false;
                        }
                    }
                    
                    // 按作者过滤
                    if (authorId != null) {
                        if (achievement.getAuthorIds() == null ||
                            !achievement.getAuthorIds().contains(authorId)) {
                            return false;
                        }
                    }
                    
                    // 按机构过滤
                    if (institutionId != null) {
                        if (achievement.getInstitutionIds() == null ||
                            !achievement.getInstitutionIds().contains(institutionId)) {
                            return false;
                        }
                    }
                    
                    // 按日期范围过滤
                    if (startDate != null && endDate != null && achievement.getPublicationDate() != null) {
                        String pubDate = achievement.getPublicationDate();
                        if (pubDate.compareTo(startDate) < 0 || pubDate.compareTo(endDate) > 0) {
                            return false;
                        }
                    }
                    
                    return true;
                })
                .map(this::toDTO)
                .collect(Collectors.toList());

        return new PageImpl<>(filtered, pageable, results.getTotalElements());
    }

    /**
     * 获取成果详情
     * 同时更新阅读次数和概念热度统计
     */
    public AchievementDTO getById(String id) {
        Achievement achievement = achievementRepository.findById(IdPrefixUtil.ensureIdPrefix(id))
                .orElseThrow(() -> new RuntimeException("成果不存在"));

        incrementReadCount(achievement);
        // updateConcept(achievement);

        return toDTO(achievement);
    }
    /**
     * 增加成果的阅读次数
     * 使用 Elasticsearch 脚本进行原子更新，避免并发覆盖问题
     */
    private void incrementReadCount(Achievement achievement) {
    updateEsFieldCount(achievement.getId(), "readCount", 1);

    if (achievement.getReadCount() == null) {
        achievement.setReadCount(1);
    } else {
        achievement.setReadCount(achievement.getReadCount() + 1);
    }
    }

    // /**
    //  * 更新概念热度统计
    //  */
    // private void updateConcept(Achievement achievement) {
    //   if (achievement.getConcepts() == null || achievement.getConcepts().isEmpty()) {
    //     return;
    //   }

    //   try {
    //     for (String concept : achievement.getConcepts()) {
    //       if (concept == null || concept.trim().isEmpty()) {
    //         continue;
    //       }

    //       int updatedRows = conceptRepository.incrementHeatCount(concept);

    //       if (updatedRows == 0) {
    //           if (!conceptRepository.existsById(concept)) {
    //               try {
    //                   conceptRepository.save(new Concept(concept, 1));
    //               } catch (Exception e) {
    //                   conceptRepository.incrementHeatCount(concept);
    //               }
    //           } else {
    //               conceptRepository.incrementHeatCount(concept);
    //           }
    //       }
    //     }
    //   } catch (Exception e) {
    //     System.err.println("Failed to update concept statistics for achievement: " + achievement.getId() + ", error: " + e.getMessage());
    //   }
    // }

    public List<AchievementDTO> getByIds(List<String> ids) {
    List<String> prefixedIds = ids.stream().map(IdPrefixUtil::ensureIdPrefix).collect(Collectors.toList());
    Iterable<Achievement> achievements = achievementRepository.findAllById(prefixedIds);
    List<AchievementDTO> dtos = new ArrayList<>();
    achievements.forEach(a -> dtos.add(toDTO(a)));
    return dtos;
    }


    public void incrementFavouriteCount(String achievementId) {
    updateEsFieldCount(IdPrefixUtil.ensureIdPrefix(achievementId), "favouriteCount", 1);
    }

    public void decrementFavouriteCount(String achievementId) {
    updateEsFieldCount(IdPrefixUtil.ensureIdPrefix(achievementId), "favouriteCount", -1);
    }


    private void updateEsFieldCount(String id, String field, int delta) {
    try {
        String scriptCode;
        if (delta > 0) {
        scriptCode = String.format("if (ctx._source.%s == null) { ctx._source.%s = %d } else { ctx._source.%s += %d }", field, field, delta, field, delta);
        } 
        else {
        scriptCode = String.format("if (ctx._source.%s != null && ctx._source.%s > 0) { ctx._source.%s += %d }", field, field, field, delta);
        }

        UpdateQuery updateQuery = UpdateQuery.builder(id)
            .withScript(scriptCode)
            .withLang("painless")
            .withScriptType(ScriptType.INLINE)
            .build();

        elasticsearchOperations.update(updateQuery, IndexCoordinates.of("openalex_works"));
    } catch (Exception e) {
        e.printStackTrace();
        System.err.println("Failed to update " + field + " for achievement: " + id + ", error: " + e.getMessage());
    }
    }

    /**
     * 转换为DTO，提取作者信息
     */
    public AchievementDTO toDTO(Achievement achievement) {
    AchievementDTO dto = Achievement.toDTO(achievement);

    // Check if current user has favourited this achievement
    try {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            String email = authentication.getName();
            userRepository.findByEmail(email).ifPresent(user -> {
                boolean isFav = userCollectionRepository.existsByUserIdAndAchievementId(user.getId(), IdPrefixUtil.removeIdPrefix(achievement.getId()));
                dto.setIsFavourite(isFav);
            });
        } else {
            dto.setIsFavourite(false);
        }
    } catch (Exception e) {
        dto.setIsFavourite(false);
    }
    return dto;

    }

  public List<AchievementDTO> getPendingAchievements() {
      return achievementRepository.findByStatus(Achievement.AchievementStatus.PENDING)
              .stream()
              .map(Achievement::toDTO)
              .collect(Collectors.toList());
  }

  @Transactional
  public Achievement approveAchievement(String achievementId, String adminId) {
      Achievement achievement = achievementRepository.findById(achievementId)
              .orElseThrow(() -> new RuntimeException("成果不存在"));

      User admin = userRepository.findById(adminId)
              .orElseThrow(() -> new RuntimeException("管理员不存在"));

      achievement.setStatus(Achievement.AchievementStatus.APPROVED);
      return achievementRepository.save(achievement);
  }

  @Transactional
  public Achievement rejectAchievement(String achievementId, String adminId, String reason) {
      Achievement achievement = achievementRepository.findById(achievementId)
              .orElseThrow(() -> new RuntimeException("成果不存在"));

      User admin = userRepository.findById(adminId)
              .orElseThrow(() -> new RuntimeException("管理员不存在"));

      achievement.setStatus(Achievement.AchievementStatus.REJECTED);
      return achievementRepository.save(achievement);
  }

}
