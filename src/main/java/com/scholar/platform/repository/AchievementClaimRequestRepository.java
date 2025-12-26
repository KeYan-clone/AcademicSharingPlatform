package com.scholar.platform.repository;

import com.scholar.platform.entity.AchievementClaimRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AchievementClaimRequestRepository extends JpaRepository<AchievementClaimRequest, String> {

  List<AchievementClaimRequest> findByUserId(String userId);

  List<AchievementClaimRequest> findByAchievementId(String achievementId);

  boolean existsByUserIdAndAchievementId(String userId, String achievementId);

  Optional<AchievementClaimRequest> findByUserIdAndAchievementIdAndStatus(
      String userId, String achievementId, AchievementClaimRequest.ClaimStatus status);
}
