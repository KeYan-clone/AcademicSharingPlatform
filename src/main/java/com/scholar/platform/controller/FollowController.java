package com.scholar.platform.controller;

import com.scholar.platform.dto.ApiResponse;
import com.scholar.platform.entity.Follow;
import com.scholar.platform.service.FollowService;
import com.scholar.platform.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/social")
@RequiredArgsConstructor
@Tag(name = "关注管理", description = "用户关注和粉丝管理接口")
@SecurityRequirement(name = "Bearer Authentication")
public class FollowController {

  private final FollowService followService;
  private final UserRepository userRepository;

  @PostMapping("/follow/{userId}")
  @Operation(summary = "关注一个学者", description = "关注指定学者")
  public ResponseEntity<ApiResponse<Void>> followUser(
      Authentication authentication,
      @Parameter(description = "被关注学者ID") @PathVariable String userId) {
    String email = authentication.getName();
    String followerId = userRepository.findByEmail(email)
            .map(u -> u.getId())
            .orElseThrow(() -> new RuntimeException("当前用户不存在"));
    followService.follow(followerId, userId);
    return ResponseEntity.ok(ApiResponse.success("关注成功", null));
  }

  @DeleteMapping("/follow/{userId}")
  @Operation(summary = "取消关注一个学者", description = "取消关注指定学者")
  public ResponseEntity<ApiResponse<Void>> unfollowUser(
      Authentication authentication,
      @Parameter(description = "被取消关注学者ID") @PathVariable String userId) {
    String email = authentication.getName();
    String followerId = userRepository.findByEmail(email)
            .map(u -> u.getId())
            .orElseThrow(() -> new RuntimeException("当前用户不存在"));
    followService.unfollow(followerId, userId);
    return ResponseEntity.ok(ApiResponse.success("取消关注成功", null));
  }

  @GetMapping("/followers/{userId}")
  @Operation(summary = "查看某个学者的粉丝列表", description = "查询指定学者的粉丝列表")
  public ResponseEntity<ApiResponse<Object>> getFollowers(
      @Parameter(description = "学者ID") @PathVariable String userId) {
    List<java.util.Map<String, Object>> followers = followService.getFollowersInfo(userId);
    int total = followers.size();
    return ResponseEntity.ok(ApiResponse.success(new java.util.HashMap<String, Object>() {{
      put("followers", followers);
      put("total", total);
    }}));
  }

  @GetMapping("/following/{userId}")
  @Operation(summary = "查看某个学者关注的人列表", description = "查询指定学者关注的人列表")
  public ResponseEntity<ApiResponse<Object>> getFollowing(
      @Parameter(description = "学者ID") @PathVariable String userId) {
    List<java.util.Map<String, Object>> following = followService.getFollowingInfo(userId);
    int total = following.size();
    return ResponseEntity.ok(ApiResponse.success(new java.util.HashMap<String, Object>() {{
      put("following", following);
      put("total", total);
    }}));
  }

  @GetMapping("/{userId}/follower-count")
  @Operation(summary = "获取粉丝数", description = "统计指定用户的粉丝数量")
  public ResponseEntity<ApiResponse<Long>> getFollowerCount(
      @Parameter(description = "用户ID") @PathVariable String userId) {
    long count = followService.getFollowerCount(userId);
    return ResponseEntity.ok(ApiResponse.success(count));
  }

  @GetMapping("/{userId}/following-count")
  @Operation(summary = "获取关注数", description = "统计指定用户关注的人数")
  public ResponseEntity<ApiResponse<Long>> getFollowingCount(
      @Parameter(description = "用户ID") @PathVariable String userId) {
    long count = followService.getFollowingCount(userId);
    return ResponseEntity.ok(ApiResponse.success(count));
  }
}
