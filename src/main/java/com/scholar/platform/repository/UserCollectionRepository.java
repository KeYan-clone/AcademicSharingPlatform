package com.scholar.platform.repository;

import com.scholar.platform.entity.UserCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserCollectionRepository extends JpaRepository<UserCollection, UserCollection.UserCollectionId> {

  List<UserCollection> findByUserId(String userId);

  List<UserCollection> findByAchievementId(String achievementId);

  boolean existsByUserIdAndAchievementId(String userId, String achievementId);
}
