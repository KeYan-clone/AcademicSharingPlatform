package com.scholar.platform.service;

import com.scholar.platform.entity.KnowledgeBase;
import com.scholar.platform.repository.KnowledgeBaseRepository;
import com.scholar.platform.repository.KnowledgeDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

  private final KnowledgeBaseRepository knowledgeBaseRepository;
  private final KnowledgeDocumentRepository knowledgeDocumentRepository;
  private final AiServiceClient aiServiceClient;

  @Value("${storage.knowledge-base-root:storage/knowledge-base}")
  private String kbRoot;

  public List<KnowledgeBase> listByUser(String userId) {
    return knowledgeBaseRepository.findByUserIdOrderByCreatedAtDesc(userId);
  }

  @Transactional
  public KnowledgeBase create(String userId, String name, String description, KnowledgeBase.Visibility visibility) {
    KnowledgeBase kb = new KnowledgeBase();
    kb.setId(UUID.randomUUID().toString());
    kb.setUserId(userId);
    kb.setName(name);
    kb.setDescription(description);
    kb.setVisibility(visibility == null ? KnowledgeBase.Visibility.PRIVATE : visibility);

    // 基于ID创建存储目录后再持久化，避免 storage_path 为空
    Path kbPath = resolveBasePath(userId, kb.getId());
    try {
      Files.createDirectories(kbPath);
    } catch (IOException e) {
      throw new RuntimeException("创建知识库存储目录失败: " + e.getMessage(), e);
    }
    kb.setStoragePath(kbPath.toAbsolutePath().toString());
    return knowledgeBaseRepository.save(kb);
  }

  public KnowledgeBase getOwnedOrThrow(String userId, String kbId) {
    KnowledgeBase kb = knowledgeBaseRepository.findByIdAndUserId(kbId, userId)
        .orElseThrow(() -> new RuntimeException("知识库不存在或无访问权限"));
    if (kb.getStoragePath() == null || kb.getStoragePath().isBlank()) {
      Path kbPath = resolveBasePath(userId, kbId);
      try {
        Files.createDirectories(kbPath);
      } catch (IOException e) {
        throw new RuntimeException("无法初始化知识库目录: " + e.getMessage(), e);
      }
      kb.setStoragePath(kbPath.toAbsolutePath().toString());
      knowledgeBaseRepository.save(kb);
    }
    return kb;
  }

  @Transactional
  public KnowledgeBase update(String userId, String kbId, String name, String description, KnowledgeBase.Visibility visibility) {
    KnowledgeBase kb = getOwnedOrThrow(userId, kbId);
    if (name != null && !name.isBlank()) {
      kb.setName(name);
    }
    if (description != null) {
      kb.setDescription(description);
    }
    if (visibility != null) {
      kb.setVisibility(visibility);
    }
    return knowledgeBaseRepository.save(kb);
  }

  @Transactional
  public void delete(String userId, String kbId) {
    KnowledgeBase kb = getOwnedOrThrow(userId, kbId);
    knowledgeDocumentRepository.deleteAll(knowledgeDocumentRepository.findByKnowledgeBaseIdOrderByCreatedAtDesc(kbId));
    knowledgeBaseRepository.delete(kb);
    if (kb.getStoragePath() != null) {
      FileSystemUtils.deleteRecursively(Path.of(kb.getStoragePath()).toFile());
    }
    // 清理 ai_service 中的向量残留
    try {
      aiServiceClient.deleteKnowledgeBase(userId, kbId);
    } catch (Exception ignored) {
      // 已记录警告，不影响主流程
    }
  }

  public Path resolveBasePath(String userId, String kbId) {
    return Path.of(kbRoot, userId, kbId);
  }
}
