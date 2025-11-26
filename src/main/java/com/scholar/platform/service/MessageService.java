package com.scholar.platform.service;

import com.scholar.platform.dto.MessageRequest;
import com.scholar.platform.entity.DirectMessage;
import com.scholar.platform.entity.User;
import com.scholar.platform.repository.DirectMessageRepository;
import com.scholar.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

  private final DirectMessageRepository messageRepository;
  private final UserRepository userRepository;

  @Transactional
  public DirectMessage sendMessage(String senderId, MessageRequest request) {
    User sender = userRepository.findById(senderId)
        .orElseThrow(() -> new RuntimeException("发送者不存在"));
    User recipient = userRepository.findById(request.getRecipientId())
        .orElseThrow(() -> new RuntimeException("接收者不存在"));

    DirectMessage message = new DirectMessage();
    message.setSender(sender);
    message.setRecipient(recipient);
    message.setContent(request.getContent());

    return messageRepository.save(message);
  }

  public List<DirectMessage> getConversation(String userId1, String userId2) {
    return messageRepository.findConversation(userId1, userId2);
  }

  public List<DirectMessage> getUserMessages(String userId) {
    return messageRepository.findBySenderIdOrRecipientIdOrderBySentAtDesc(userId, userId);
  }
}
