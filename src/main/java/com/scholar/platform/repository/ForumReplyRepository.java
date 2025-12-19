package com.scholar.platform.repository;

import com.scholar.platform.entity.ForumReply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForumReplyRepository extends JpaRepository<ForumReply, String> {

  Page<ForumReply> findByPostId(String postId, Pageable pageable);

  @Query("SELECT r FROM ForumReply r JOIN FETCH r.author WHERE r.post.id = :postId ORDER BY r.createdAt ASC")
  List<ForumReply> findByPostIdWithAuthor(@Param("postId") String postId);
  
  List<ForumReply> findByAuthorId(String authorId);
}
