package com.scholar.platform.repository;

import com.scholar.platform.entity.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, String> {

  List<KnowledgeDocument> findByKnowledgeBaseIdOrderByCreatedAtDesc(String knowledgeBaseId);

  Optional<KnowledgeDocument> findByIdAndUserId(String id, String userId);

  boolean existsByIdAndUserId(String id, String userId);
}
