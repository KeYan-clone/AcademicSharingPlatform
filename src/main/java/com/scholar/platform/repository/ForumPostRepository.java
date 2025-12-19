package com.scholar.platform.repository;

import com.scholar.platform.entity.ForumPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ForumPostRepository extends JpaRepository<ForumPost, String> {

  @EntityGraph(attributePaths = {"author", "board"})
  Page<ForumPost> findByBoardId(String boardId, Pageable pageable);

  @EntityGraph(attributePaths = {"author", "board"})
  Page<ForumPost> findByAuthorId(String authorId, Pageable pageable);

  @EntityGraph(attributePaths = {"author", "board"})
  Page<ForumPost> findByTitleContaining(String title, Pageable pageable);

  @Query(value = "SELECT p, COUNT(r), MAX(r.createdAt) " +
         "FROM ForumPost p " +
         "LEFT JOIN FETCH p.author " +  // 预加载作者
         "LEFT JOIN FETCH p.board " +   // 预加载板块
         "LEFT JOIN ForumReply r ON p.id = r.post.id " +
         "WHERE (:boardId IS NULL OR p.board.id = :boardId) " +
         "GROUP BY p.id",
         countQuery = "SELECT COUNT(p) FROM ForumPost p WHERE (:boardId IS NULL OR p.board.id = :boardId)")
  Page<Object[]> findPostsWithStats(@Param("boardId") String boardId, Pageable pageable);
}
