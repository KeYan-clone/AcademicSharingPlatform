package com.scholar.platform.service;

import com.scholar.platform.dto.AchievementDTO;
import com.scholar.platform.entity.Achievement;
import com.scholar.platform.entity.User;
import com.scholar.platform.repository.AchievementRepository;
import com.scholar.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.ScriptType;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AchievementService {

  private final AchievementRepository achievementRepository;
  private final UserRepository userRepository;

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
   * 按时间范围检索
   */
  public Page<AchievementDTO> searchByDateRange(String startDate, String endDate, Pageable pageable) {
    if (startDate == null || endDate == null) {
      throw new IllegalArgumentException("起止时间不能为空");
    }
    return achievementRepository.findByPublicationDateBetween(startDate, endDate, pageable)
        .map(this::toDTO);
  }

  /**
   * 高级检索：支持关键词、概念、时间范围、作者和机构的组合检索
   * @param keyword 关键词（模糊搜索标题和概念）
   * @param field 学科领域/概念（精确匹配，由用户从下拉列表选择）
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @param authorName 作者姓名（精确匹配）
   * @param institutionName 机构名称（精确匹配）
   * @param pageable 分页参数
   */
  public Page<AchievementDTO> advancedSearch(String keyword, String field, 
                                              String startDate, String endDate, 
                                              String authorName, String institutionName,
                                              Pageable pageable) {
    // 1. 机构检索
    if (institutionName != null && !institutionName.trim().isEmpty()) {
      Page<Institution> institutions = institutionRepository.findByDisplayName(institutionName, Pageable.ofSize(1));
      if (institutions.hasContent()) {
        String institutionId = institutions.getContent().get(0).getId();
        return achievementRepository.findByInstitutionIds(institutionId, pageable).map(this::toDTO);
      } else {
        return Page.empty(pageable);
      }
    }

    // 2. 作者检索
    if (authorName != null && !authorName.trim().isEmpty()) {
      Page<Author> authors = authorRepository.findByDisplayName(authorName, Pageable.ofSize(1));
      if (authors.hasContent()) {
        String authorId = authors.getContent().get(0).getId();
        return achievementRepository.findByAuthorIds(authorId, pageable).map(this::toDTO);
      } else {
        return Page.empty(pageable);
      }
    }

    // 如果有时间范围
    if (startDate != null && endDate != null) {
      // 时间 + 概念/领域（精确匹配）
      if (field != null && !field.trim().isEmpty()) {
        return achievementRepository.findByDateRangeAndConceptExactMatch(
            startDate, endDate, field, pageable).map(this::toDTO);
      }
      // 时间 + 关键词（模糊匹配，支持空格）
      else if (keyword != null && !keyword.trim().isEmpty()) {
        return achievementRepository.searchByDateRangeAndKeywordWithSpaceSupport(
            startDate, endDate, keyword, pageable).map(this::toDTO);
      }
      // 仅时间
      else {
        return searchByDateRange(startDate, endDate, pageable);
      }
    }
    // 无时间范围，使用原有检索逻辑
    else if (field != null && !field.trim().isEmpty()) {
      // field 始终使用精确匹配
      return searchByConceptsExact(field, pageable);
    } else if (keyword != null && !keyword.trim().isEmpty()) {
      return searchByKeyword(keyword, pageable);
    } else {
      throw new IllegalArgumentException("请输入检索内容");
    }
  }

  /**
   * 获取成果详情
   * 同时更新阅读次数和概念热度统计
   */
  public AchievementDTO getById(String id) {
    Achievement achievement = achievementRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("成果不存在"));

    incrementReadCount(achievement);
    updateConcept(achievement);
    
    return toDTO(achievement);
  }

  public List<AchievementDTO> getPendingAchievements() {
    return achievementRepository.findByStatus(Achievement.AchievementStatus.PENDING)
        .stream()
        .map(this::toDTO)
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

  private AchievementDTO toDTO(Achievement achievement) {
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
