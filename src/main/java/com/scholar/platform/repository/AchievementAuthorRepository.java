package com.scholar.platform.repository;

import com.scholar.platform.entity.AchievementAuthor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AchievementAuthorRepository extends JpaRepository<AchievementAuthor, String> {

    List<AchievementAuthor> findByAchievementId(String achievementId);

    List<AchievementAuthor> findByAuthorUserId(String userId);

    List<AchievementAuthor> findByAuthorNameContaining(String authorName);
}