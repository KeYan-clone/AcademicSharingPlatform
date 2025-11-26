package com.scholar.platform.service;

import com.scholar.platform.entity.Follow;
import com.scholar.platform.entity.User;
import com.scholar.platform.repository.FollowRepository;
import com.scholar.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FollowService {

  private final FollowRepository followRepository;
  private final UserRepository userRepository;

  @Transactional
  public void follow(String followerId, String followingId) {
    if (followerId.equals(followingId)) {
      throw new RuntimeException("不能关注自己");
    }

    if (followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
      throw new RuntimeException("已经关注过了");
    }

    User follower = userRepository.findById(followerId)
        .orElseThrow(() -> new RuntimeException("用户不存在"));
    User following = userRepository.findById(followingId)
        .orElseThrow(() -> new RuntimeException("被关注用户不存在"));

    Follow follow = new Follow();
    follow.setFollowerId(followerId);
    follow.setFollowingId(followingId);
    follow.setFollower(follower);
    follow.setFollowing(following);

    followRepository.save(follow);
  }

  @Transactional
  public void unfollow(String followerId, String followingId) {
    Follow.FollowId id = new Follow.FollowId(followerId, followingId);
    followRepository.deleteById(id);
  }

  public List<Follow> getFollowers(String userId) {
    return followRepository.findByFollowingId(userId);
  }

  public List<Follow> getFollowing(String userId) {
    return followRepository.findByFollowerId(userId);
  }

  public long getFollowerCount(String userId) {
    return followRepository.countByFollowingId(userId);
  }

  public long getFollowingCount(String userId) {
    return followRepository.countByFollowerId(userId);
  }
}
