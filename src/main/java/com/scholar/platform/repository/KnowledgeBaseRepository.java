package com.scholar.platform.repository;

import com.scholar.platform.entity.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, String> {

  List<KnowledgeBase> findByUserIdOrderByCreatedAtDesc(String userId);

  Optional<KnowledgeBase> findByIdAndUserId(String id, String userId);

  boolean existsByIdAndUserId(String id, String userId);
}
