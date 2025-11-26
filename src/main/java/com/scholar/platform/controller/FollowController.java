package com.scholar.platform.controller;

import com.scholar.platform.dto.ApiResponse;
import com.scholar.platform.entity.Follow;
import com.scholar.platform.service.FollowService;
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
@RequestMapping("/follows")
@RequiredArgsConstructor
@Tag(name = "关注管理", description = "用户关注和粉丝管理接口")
@SecurityRequirement(name = "Bearer Authentication")
public class FollowController {

  private final FollowService followService;

  @PostMapping("/{followingId}")
  @Operation(summary = "关注用户", description = "关注指定用户")
  public ResponseEntity<ApiResponse<Void>> followUser(
      Authentication authentication,
      @Parameter(description = "被关注用户ID") @PathVariable String followingId) {
    String followerId = authentication.getName();
    followService.follow(followerId, followingId);
    return ResponseEntity.ok(ApiResponse.success("关注成功", null));
  }

  @DeleteMapping("/{followingId}")
  @Operation(summary = "取消关注", description = "取消关注指定用户")
  public ResponseEntity<ApiResponse<Void>> unfollowUser(
      Authentication authentication,
      @Parameter(description = "被关注用户ID") @PathVariable String followingId) {
    String followerId = authentication.getName();
    followService.unfollow(followerId, followingId);
    return ResponseEntity.ok(ApiResponse.success("已取消关注", null));
  }

  @GetMapping("/{userId}/followers")
  @Operation(summary = "获取粉丝列表", description = "查询指定用户的粉丝")
  public ResponseEntity<ApiResponse<List<Follow>>> getFollowers(
      @Parameter(description = "用户ID") @PathVariable String userId) {
    List<Follow> followers = followService.getFollowers(userId);
    return ResponseEntity.ok(ApiResponse.success(followers));
  }

  @GetMapping("/{userId}/following")
  @Operation(summary = "获取关注列表", description = "查询指定用户关注的人")
  public ResponseEntity<ApiResponse<List<Follow>>> getFollowing(
      @Parameter(description = "用户ID") @PathVariable String userId) {
    List<Follow> following = followService.getFollowing(userId);
    return ResponseEntity.ok(ApiResponse.success(following));
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
