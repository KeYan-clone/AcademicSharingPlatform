package com.scholar.platform.service;

import com.scholar.platform.dto.AchievementDTO;
import com.scholar.platform.entity.Achievement;
import com.scholar.platform.repository.AchievementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AchievementService {

  private final AchievementRepository achievementRepository;

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
