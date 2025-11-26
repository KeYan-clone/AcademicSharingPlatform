package com.scholar.platform.repository;

import com.scholar.platform.entity.ForumReply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForumReplyRepository extends JpaRepository<ForumReply, String> {

  Page<ForumReply> findByPostId(String postId, Pageable pageable);

  List<ForumReply> findByAuthorId(String authorId);
}
