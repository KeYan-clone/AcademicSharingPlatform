package com.scholar.platform.repository;

import com.scholar.platform.entity.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Follow.FollowId> {

  List<Follow> findByFollowerId(String followerId);

  List<Follow> findByFollowingId(String followingId);

  long countByFollowerId(String followerId);

  long countByFollowingId(String followingId);

  boolean existsByFollowerIdAndFollowingId(String followerId, String followingId);
}
