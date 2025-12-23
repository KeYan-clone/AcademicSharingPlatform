package com.scholar.platform.service;

import com.scholar.platform.dto.AchievementDTO;
import com.scholar.platform.entity.Achievement;
import com.scholar.platform.repository.AchievementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AchievementService {

  private final AchievementRepository achievementRepository;

  /**
   * 通过关键词搜索（标题或概念）
   * 使用 match 查询，支持包含空格的关键词如 "artificial intelligence"
   */
  public Page<AchievementDTO> searchByKeyword(String keyword, Pageable pageable) {
    if (keyword == null || keyword.trim().isEmpty()) {
      throw new IllegalArgumentException("请输入检索内容");
    }
    return achievementRepository.searchByKeywordWithSpaceSupport(keyword, pageable)
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
   * 高级检索：支持关键词、概念和时间范围的组合检索
   * @param keyword 关键词（模糊搜索标题和概念）
   * @param field 学科领域/概念（精确匹配，由用户从下拉列表选择）
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @param pageable 分页参数
   */
  public Page<AchievementDTO> advancedSearch(String keyword, String field, 
                                              String startDate, String endDate, 
                                              Pageable pageable) {
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
   */
  public AchievementDTO getById(String id) {
    Achievement achievement = achievementRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("成果不存在"));
    return toDTO(achievement);
  }

  /**
   * 转换为DTO，提取作者信息
   */
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

    if (achievement.getAuthorships() != null) {
      List<AchievementDTO.AuthorInfo> authors = achievement.getAuthorships().stream()
          .filter(authorship -> authorship.getAuthor() != null)
          .map(authorship -> new AchievementDTO.AuthorInfo(
              authorship.getAuthor().getId(),
              authorship.getAuthor().getDisplayName()
          ))
          .collect(Collectors.toList());
      dto.setAuthors(authors);
    }

    return dto;
  }
}
