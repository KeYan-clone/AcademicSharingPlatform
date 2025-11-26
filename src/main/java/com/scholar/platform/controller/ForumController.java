package com.scholar.platform.controller;

import com.scholar.platform.dto.*;
import com.scholar.platform.entity.ForumPost;
import com.scholar.platform.entity.ForumReply;
import com.scholar.platform.service.ForumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/forum")
@RequiredArgsConstructor
@Tag(name = "论坛管理", description = "论坛帖子和回复管理接口")
@SecurityRequirement(name = "Bearer Authentication")
public class ForumController {

  private final ForumService forumService;

  @PostMapping("/posts")
  @Operation(summary = "发布帖子", description = "在指定板块创建新帖子")
  public ResponseEntity<ApiResponse<ForumPost>> createPost(
      Authentication authentication,
      @Valid @RequestBody PostCreateRequest request) {
    String authorId = authentication.getName();
    ForumPost post = forumService.createPost(authorId, request);
    return ResponseEntity.ok(ApiResponse.success("发帖成功", post));
  }

  @GetMapping("/boards/{boardId}/posts")
  @Operation(summary = "获取板块帖子", description = "查询指定板块的帖子列表")
  public ResponseEntity<ApiResponse<PageResponse<PostDTO>>> getPostsByBoard(
      @Parameter(description = "板块ID") @PathVariable String boardId,
      @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
    Pageable pageable = PageRequest.of(page, size);
    Page<PostDTO> posts = forumService.getPostsByBoard(boardId, pageable);
    return ResponseEntity.ok(ApiResponse.success(PageResponse.of(posts)));
  }

  @GetMapping("/posts/{postId}")
  @Operation(summary = "获取帖子详情", description = "查看指定帖子的详细内容")
  public ResponseEntity<ApiResponse<PostDTO>> getPostById(
      @Parameter(description = "帖子ID") @PathVariable String postId) {
    PostDTO post = forumService.getPostById(postId);
    return ResponseEntity.ok(ApiResponse.success(post));
  }

  @PostMapping("/replies")
  @Operation(summary = "发布回复", description = "在指定帖子下添加回复")
  public ResponseEntity<ApiResponse<ForumReply>> createReply(
      Authentication authentication,
      @Valid @RequestBody ReplyCreateRequest request) {
    String authorId = authentication.getName();
    ForumReply reply = forumService.createReply(authorId, request);
    return ResponseEntity.ok(ApiResponse.success("回复成功", reply));
  }

  @GetMapping("/posts/{postId}/replies")
  @Operation(summary = "获取帖子回复", description = "查询指定帖子的所有回复")
  public ResponseEntity<ApiResponse<PageResponse<ForumReply>>> getRepliesByPost(
      @Parameter(description = "帖子ID") @PathVariable String postId,
      @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
    Pageable pageable = PageRequest.of(page, size);
    Page<ForumReply> replies = forumService.getRepliesByPost(postId, pageable);
    return ResponseEntity.ok(ApiResponse.success(PageResponse.of(replies)));
  }
}
