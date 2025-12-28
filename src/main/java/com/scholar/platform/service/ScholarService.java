package com.scholar.platform.service;

import com.scholar.platform.dto.ScholarDTO;
import com.scholar.platform.entity.AchievementAuthor;
import com.scholar.platform.entity.Scholar;
import com.scholar.platform.repository.AchievementAuthorRepository;
import com.scholar.platform.repository.ScholarRepository;
import com.scholar.platform.repository.UserRepository;
import com.scholar.platform.util.Utils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScholarService {

  private final ScholarRepository scholarRepository;
  private final UserRepository userRepository;
  private final AchievementAuthorRepository achievementAuthorRepository;

  /**
   * 根据姓名、机构、领域搜索学者
   */
  public List<ScholarDTO> searchScholar(Optional<String> name, Optional<String> organization, Optional<String> field) {
    List<Scholar> scholars;

    // 根据提供的条件进行搜索
    if (name.isPresent()) {
      scholars = scholarRepository.findByPublicNameContaining(name.get());
    } else if (organization.isPresent()) {
      scholars = scholarRepository.findByOrganizationContaining(organization.get());
    } else {
      // 如果都没有提供，返回所有学者
      scholars = scholarRepository.findAll();
    }

    // 如果提供了机构过滤条件，进一步过滤
    if (organization.isPresent() && name.isPresent()) {
      scholars = scholars.stream()
          .filter(s -> s.getOrganization() != null &&
              s.getOrganization().contains(organization.get()))
          .collect(Collectors.toList());
    }

    // 转换为DTO
    return scholars.stream()
        .map(ScholarDTO::from)
        .collect(Collectors.toList());
  }

  /**
   * 查找用户的所有合作者
   * 合作者定义：与该用户共同作者过成果的其他用户
   *
   */
  public List<ScholarDTO> findAllCollaborators(String userId) {
    // 获取用户的所有成果
    List<AchievementAuthor> userAuthorships = achievementAuthorRepository.findByAuthorUserId(userId);

    // 提取成果ID列表
    List<String> achievementIds = userAuthorships.stream()
        .map(aa -> Utils.getAchievement(aa.getAchievementId()).getId())
        .collect(Collectors.toList());

    // 找出这些成果的所有其他作者
    List<String> collaboratorIds = achievementIds.stream()
        .flatMap(achievementId -> achievementAuthorRepository.findByAchievementId(achievementId).stream())
        .filter(aa -> aa.getAuthorUser() != null)
        .map(aa -> aa.getAuthorUser().getId())
        .filter(id -> !id.equals(userId)) // 排除自己
        .distinct()
        .collect(Collectors.toList());

    // 转换为ScholarDTO
    return collaboratorIds.stream()
        .map(scholarRepository::findById)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(ScholarDTO::from)
        .collect(Collectors.toList());
  }

  public ScholarDTO getScholarProfile(String userId) {
    Scholar scholar = scholarRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("学者信息不存在"));
    return ScholarDTO.from(scholar);
  }

  @Transactional
  public ScholarDTO updateScholarProfile(String userId, ScholarDTO dto) {
     userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("用户不存在"));

    Scholar scholar = scholarRepository.findById(userId).orElse(new Scholar());
    scholar.setUserId(userId);
    scholar.setPublicName(dto.getPublicName());
    scholar.setOrganization(dto.getOrganization());
    scholar.setTitle(dto.getTitle());
    scholar.setBio(dto.getBio());
    scholar.setAvatarUrl(dto.getAvatarUrl());

    scholar = scholarRepository.save(scholar);
    return ScholarDTO.from(scholar);
  }
}
