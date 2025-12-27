package com.scholar.platform.controller;

import com.scholar.platform.dto.ApiResponse;
import com.scholar.platform.dto.forum.*;
import com.scholar.platform.entity.ForumPost;
import com.scholar.platform.entity.ForumReply;
import com.scholar.platform.service.ForumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/social/forum")
@RequiredArgsConstructor
@Tag(name = "论坛管理", description = "论坛帖子和回复管理接口")
@SecurityRequirement(name = "Bearer Authentication")
public class ForumController {

  private final ForumService forumService;

  @GetMapping("/posts")
  @Operation(summary = "获取帖子列表", description = "查询论坛帖子列表，支持按板块筛选")
  public ResponseEntity<ApiResponse<Map<String, Object>>> getPosts(
      @Parameter(description = "板块ID") @RequestParam(required = false) String boardId,
      @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
    
    List<PostListItemDTO> posts = forumService.getPosts(boardId, page, size);
    return ResponseEntity.ok(ApiResponse.success(Collections.singletonMap("posts", posts)));
  }

  @PostMapping("/posts")
  @Operation(summary = "发布帖子", description = "在指定板块创建新帖子")
  public ResponseEntity<ApiResponse<ForumPost>> createPost(
      Authentication authentication,
      @Valid @RequestBody CreatePostRequest request) {
    
    String email = authentication.getName(); 
    
    ForumPost post = forumService.createPost(email, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("发帖成功", post));
  }

  @GetMapping("/posts/{id}")
  @Operation(summary = "获取帖子详情", description = "查看帖子详情及回复列表")
  public ResponseEntity<ApiResponse<Map<String, Object>>> getPostDetail(
      //@Parameter(description = "帖子ID") @PathVariable("id") String postId) {
    
    //PostDetailDTO postDetail = forumService.getPostDetail(postId);
    //return ResponseEntity.ok(ApiResponse.success(postDetail));
    @Parameter(description = "帖子ID") @PathVariable("id") String postId) {
    
    // 获取原始 DTO
    PostDetailDTO dto = forumService.getPostDetail(postId);
    
    // 2. 构造符合文档要求的 Map 结构
    Map<String, Object> responseData = new HashMap<>();
    
    // 提取回复列表放入 "replies"
    responseData.put("replies", dto.getReplies());
    
    // 将 DTO 本身放入 "post" (为了避免数据重复，先把 DTO 里的 replies 清空)
    dto.setReplies(null); 
    responseData.put("post", dto);

    return ResponseEntity.ok(ApiResponse.success(responseData));
  }

  @PostMapping("/posts/{id}/reply")
  @Operation(summary = "回复帖子", description = "对指定帖子进行回复")
  public ResponseEntity<ApiResponse<ForumReply>> createReply(
      Authentication authentication,
      @Parameter(description = "帖子ID") @PathVariable("id") String postId,
      @Valid @RequestBody CreateReplyRequest request) {
    
    String email = authentication.getName();
    ForumReply reply = forumService.createReply(email, postId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("回复成功", reply));
  }
}