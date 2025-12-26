package com.scholar.platform.service;

import com.scholar.platform.dto.AchievementDTO;
import com.scholar.platform.entity.UserCollection;
import com.scholar.platform.repository.UserCollectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserCollectionService {

  private final UserCollectionRepository userCollectionRepository;
  private final AchievementService achievementService;

  public List<AchievementDTO> getMyCollections(String userId) {
    List<UserCollection> collections = userCollectionRepository.findByUserId(userId);
    List<String> achievementIds = collections.stream()
        .map(UserCollection::getAchievementId)
        .collect(Collectors.toList());
    
    if (achievementIds.isEmpty()) {
      return List.of();
    }
    
    return achievementService.getByIds(achievementIds);
  }

  @Transactional
  public void addCollection(String userId, String achievementId) {
    if (userCollectionRepository.existsByUserIdAndAchievementId(userId, achievementId)) {
      throw new IllegalArgumentException("已在收藏夹中");
    }

    UserCollection collection = new UserCollection();
    collection.setUserId(userId);
    collection.setAchievementId(achievementId);
    userCollectionRepository.save(collection);

    achievementService.incrementFavouriteCount(achievementId);
  }

  @Transactional
  public void removeCollection(String userId, String achievementId) {
    if (!userCollectionRepository.existsByUserIdAndAchievementId(userId, achievementId)) {
      return;
    }

    UserCollection.UserCollectionId id = new UserCollection.UserCollectionId(userId, achievementId);
    userCollectionRepository.deleteById(id);

    achievementService.decrementFavouriteCount(achievementId);
  }
}
