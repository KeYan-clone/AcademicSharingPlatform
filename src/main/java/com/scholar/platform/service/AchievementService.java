package com.scholar.platform.service;

import com.scholar.platform.dto.AchievementDTO;
import com.scholar.platform.entity.Achievement;
import com.scholar.platform.entity.User;
import com.scholar.platform.repository.AchievementRepository;
import com.scholar.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AchievementService {

  private final AchievementRepository achievementRepository;
  private final UserRepository userRepository;

  public Page<AchievementDTO> searchByTitle(String title, Pageable pageable) {
    return achievementRepository.findByTitleContaining(title, pageable)
        .map(this::toDTO);
  }

  public Page<AchievementDTO> getByType(Achievement.AchievementType type, Pageable pageable) {
    return achievementRepository.findByType(type, pageable)
        .map(this::toDTO);
  }

  public AchievementDTO getById(String id) {
    Achievement achievement = achievementRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("成果不存在"));
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
    dto.setType(achievement.getType().name());
    dto.setTitle(achievement.getTitle());
    dto.setPublicationYear(achievement.getPublicationYear());
    dto.setAbstractText(achievement.getAbstractText());
    dto.setDoi(achievement.getDoi());
    dto.setPublicationVenue(achievement.getPublicationVenue());
    dto.setCitationCount(achievement.getCitationCount());
    dto.setCreatedAt(achievement.getCreatedAt());
    return dto;
  }
}
