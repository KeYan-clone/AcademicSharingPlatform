package com.scholar.platform.controller;

import com.scholar.platform.dto.ApiResponse;
import com.scholar.platform.dto.MessageRequest;
import com.scholar.platform.entity.DirectMessage;
import com.scholar.platform.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
@Tag(name = "私信管理", description = "用户私信发送和查询接口")
@SecurityRequirement(name = "Bearer Authentication")
public class MessageController {

  private final MessageService messageService;

  @PostMapping("/send")
  @Operation(summary = "发送私信", description = "向指定用户发送私信")
  public ResponseEntity<ApiResponse<DirectMessage>> sendMessage(
      Authentication authentication,
      @Valid @RequestBody MessageRequest request) {
    String senderId = authentication.getName();
    DirectMessage message = messageService.sendMessage(senderId, request);
    return ResponseEntity.ok(ApiResponse.success("发送成功", message));
  }

  @GetMapping("/conversation/{userId}")
  @Operation(summary = "获取对话记录", description = "查询与指定用户的私信对话")
  public ResponseEntity<ApiResponse<List<DirectMessage>>> getConversation(
      Authentication authentication,
      @Parameter(description = "对方用户ID") @PathVariable String userId) {
    String currentUserId = authentication.getName();
    List<DirectMessage> messages = messageService.getConversation(currentUserId, userId);
    return ResponseEntity.ok(ApiResponse.success(messages));
  }

  @GetMapping("/all")
  @Operation(summary = "获取所有私信", description = "查询当前用户的所有私信记录")
  public ResponseEntity<ApiResponse<List<DirectMessage>>> getUserMessages(
      Authentication authentication) {
    String userId = authentication.getName();
    List<DirectMessage> messages = messageService.getUserMessages(userId);
    return ResponseEntity.ok(ApiResponse.success(messages));
  }
}
