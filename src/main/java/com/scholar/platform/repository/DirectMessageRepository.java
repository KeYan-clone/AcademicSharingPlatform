package com.scholar.platform.repository;

import com.scholar.platform.entity.DirectMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DirectMessageRepository extends JpaRepository<DirectMessage, String> {

  List<DirectMessage> findBySenderIdOrRecipientIdOrderBySentAtDesc(String senderId, String recipientId);

  @Query("SELECT dm FROM DirectMessage dm WHERE (dm.sender.id = :userId1 AND dm.recipient.id = :userId2) OR (dm.sender.id = :userId2 AND dm.recipient.id = :userId1) ORDER BY dm.sentAt")
  List<DirectMessage> findConversation(String userId1, String userId2);
}
