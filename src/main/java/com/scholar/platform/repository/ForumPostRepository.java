package com.scholar.platform.repository;

import com.scholar.platform.entity.ForumPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ForumPostRepository extends JpaRepository<ForumPost, String> {

  Page<ForumPost> findByBoardId(String boardId, Pageable pageable);

  Page<ForumPost> findByAuthorId(String authorId, Pageable pageable);

  Page<ForumPost> findByTitleContaining(String title, Pageable pageable);
}
