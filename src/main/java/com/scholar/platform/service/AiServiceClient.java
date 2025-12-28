package com.scholar.platform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiServiceClient {

  private final RestTemplate restTemplate;

  @Value("${ai-service.enabled:true}")
  private boolean enabled;

  @Value("${ai-service.base-url:http://localhost:8000}")
  private String baseUrl;

  /**
   * 上传文件到 ai_service，让其解析并入库向量。
   * 不会抛出异常影响主流程，失败时仅记录警告。
   */
  public boolean uploadDocument(String userId, String kbId, File file, String originalFilename) {
    if (!enabled) {
      log.info("ai-service disabled, skip upload for {}", originalFilename);
      return false;
    }
    if (file == null || !file.exists()) {
      log.warn("ai-service upload skipped: file not found {}", file);
      return false;
    }
    try {
      String encodedUser = userId == null ? "" : URLEncoder.encode(userId, StandardCharsets.UTF_8);
      String encodedKb = kbId == null ? "" : URLEncoder.encode(kbId, StandardCharsets.UTF_8);
      StringBuilder url = new StringBuilder(baseUrl).append("/embedding/upload");
      boolean hasQuery = false;
      if (!encodedUser.isEmpty()) {
        url.append(hasQuery ? "&" : "?").append("user_id=").append(encodedUser);
        hasQuery = true;
      }
      if (!encodedKb.isEmpty()) {
        url.append(hasQuery ? "&" : "?").append("kb_id=").append(encodedKb);
      }

      MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
      body.add("file", new FileSystemResource(file));
      if (originalFilename != null && !originalFilename.isBlank()) {
        body.add("original_filename", originalFilename);
      }
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.MULTIPART_FORM_DATA);

      HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
      ResponseEntity<String> response = restTemplate.postForEntity(URI.create(url.toString()), requestEntity, String.class);
      log.info("ai-service upload ok: status={} body={}", response.getStatusCode(), response.getBody());
      return response.getStatusCode().is2xxSuccessful();
    } catch (Exception e) {
      log.warn("ai-service upload failed for {}: {}", originalFilename, e.getMessage());
      return false;
    }
  }

  /**
   * 调用 ai_service 的知识库问答接口，返回答案和引用。
   */
  public Map<String, Object> qa(String userId, String kbId, String question, Integer topK) {
    Assert.hasText(question, "question must not be empty");
    if (!enabled) {
      throw new IllegalStateException("ai-service is disabled");
    }
    try {
      String encodedUser = userId == null ? "" : URLEncoder.encode(userId, StandardCharsets.UTF_8);
      StringBuilder url = new StringBuilder(baseUrl).append("/kb/qa");
      if (!encodedUser.isEmpty()) {
        url.append("?user_id=").append(encodedUser);
      }
      if (kbId != null && !kbId.isBlank()) {
        url.append(encodedUser.isEmpty() ? "?kb_id=" : "&kb_id=").append(URLEncoder.encode(kbId, StandardCharsets.UTF_8));
      }

      Map<String, Object> body = new HashMap<>();
      body.put("question", question);
      if (topK != null) {
        body.put("top_k", topK);
      }

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

      ResponseEntity<Map> response = restTemplate.postForEntity(URI.create(url.toString()), entity, Map.class);
      if (response.getStatusCode().is2xxSuccessful()) {
        return response.getBody();
      }
      throw new RuntimeException("ai-service qa failed: " + response.getStatusCode());
    } catch (Exception e) {
      log.error("ai-service qa error: {}", e.getMessage());
      throw new RuntimeException("调用 ai_service 失败: " + e.getMessage(), e);
    }
  }

  /**
   * 删除 ai_service 中对应知识库的向量，避免残留污染检索。
   */
  public void deleteKnowledgeBase(String userId, String kbId) {
    if (!enabled) {
      return;
    }
    try {
      String encodedUser = userId == null ? "" : URLEncoder.encode(userId, StandardCharsets.UTF_8);
      StringBuilder url = new StringBuilder(baseUrl).append("/kb/").append(kbId);
      if (!encodedUser.isEmpty()) {
        url.append("?user_id=").append(encodedUser);
      }
      restTemplate.delete(URI.create(url.toString()));
      log.info("ai-service kb delete ok for {}", kbId);
    } catch (Exception e) {
      log.warn("ai-service kb delete failed for {}: {}", kbId, e.getMessage());
    }
  }
}
