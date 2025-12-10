package com.scholar.platform.controller;

import com.scholar.platform.dto.ApiResponse;
import com.scholar.platform.dto.MessageRequest;
import com.scholar.platform.entity.DirectMessage;
import com.scholar.platform.service.MessageService;
import com.scholar.platform.repository.UserRepository;
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

@RequestMapping("/social/dms")
@RequiredArgsConstructor
@Tag(name = "私信管理", description = "用户私信发送和查询接口")
@SecurityRequirement(name = "Bearer Authentication")

public class MessageController {

  private final MessageService messageService;
  private final UserRepository userRepository;

  @PostMapping("/send")
  @Operation(summary = "发送私信", description = "向指定用户发送私信")
  public ResponseEntity<ApiResponse<Object>> sendMessage(
      Authentication authentication,
      @Valid @RequestBody MessageRequest request) {
    String email = authentication.getName();
    String senderId = userRepository.findByEmail(email)
            .map(u -> u.getId())
            .orElseThrow(() -> new RuntimeException("当前用户不存在"));
    DirectMessage message = messageService.sendMessage(senderId, request);
    return ResponseEntity.ok(ApiResponse.success("发送成功", new java.util.HashMap<String, Object>() {{
      put("message", message);
    }}));
  }

  @GetMapping("/conversation/{userId}")
  @Operation(summary = "获取对话记录", description = "查询与指定用户的私信对话")
  public ResponseEntity<Object> getConversation(
      Authentication authentication,
      @Parameter(description = "对方用户ID") @PathVariable String userId) {
    String email = authentication.getName();
    String currentUserId = userRepository.findByEmail(email)
            .map(u -> u.getId())
            .orElseThrow(() -> new RuntimeException("当前用户不存在"));
    List<DirectMessage> messages = messageService.getConversation(currentUserId, userId);
    List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
    for (DirectMessage msg : messages) {
      java.util.Map<String, Object> map = new java.util.HashMap<>();
      map.put("senderId", msg.getSender().getId());
      map.put("content", msg.getContent());
      map.put("timestamp", msg.getSentAt());
      result.add(map);
    }
    return ResponseEntity.ok(java.util.Collections.singletonMap("messages", result));
  }

  @GetMapping("")
  @Operation(summary = "获取私信会话列表", description = "查询当前用户的所有私信会话")
  public ResponseEntity<Object> getConversations(Authentication authentication) {
    String email = authentication.getName();
    String userId = userRepository.findByEmail(email)
            .map(u -> u.getId())
            .orElseThrow(() -> new RuntimeException("当前用户不存在"));
    // 这里简单模拟：每个会话只返回对方用户基本信息、最近一条消息内容、unreadCount=0
    List<java.util.Map<String, Object>> conversations = new java.util.ArrayList<>();
    // 这里应由 Service 返回会话聚合数据，暂用所有与我相关的消息聚合模拟
    List<DirectMessage> messages = messageService.getUserMessages(userId);
    java.util.Map<String, java.util.Map<String, Object>> convMap = new java.util.HashMap<>();
    for (DirectMessage msg : messages) {
      String otherId = msg.getSender().getId().equals(userId) ? msg.getRecipient().getId() : msg.getSender().getId();
      String otherName = msg.getSender().getId().equals(userId) ? msg.getRecipient().getUsername() : msg.getSender().getUsername();
      java.util.Map<String, Object> withUser = new java.util.HashMap<>();
      withUser.put("userId", otherId);
      withUser.put("username", otherName);
      // 只保留每个会话的最新一条消息
      if (!convMap.containsKey(otherId) || ((DirectMessage)convMap.get(otherId).get("_msg")).getSentAt().isBefore(msg.getSentAt())) {
        java.util.Map<String, Object> conv = new java.util.HashMap<>();
        conv.put("withUser", withUser);
        conv.put("latestMessage", msg.getContent());
        conv.put("unreadCount", 0);
        conv.put("_msg", msg); // 用于比较时间，最后移除
        convMap.put(otherId, conv);
      }
    }
    for (java.util.Map<String, Object> conv : convMap.values()) {
      conv.remove("_msg");
      conversations.add(conv);
    }
    return ResponseEntity.ok(java.util.Collections.singletonMap("conversations", conversations));
  }
}
