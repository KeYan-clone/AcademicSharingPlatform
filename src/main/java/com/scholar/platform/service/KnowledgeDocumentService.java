package com.scholar.platform.service;

import com.scholar.platform.entity.KnowledgeBase;
import com.scholar.platform.entity.KnowledgeDocument;
import com.scholar.platform.repository.KnowledgeDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KnowledgeDocumentService {

  private final KnowledgeDocumentRepository knowledgeDocumentRepository;
  private final KnowledgeBaseService knowledgeBaseService;
  private final AiServiceClient aiServiceClient;

  public List<KnowledgeDocument> listByKnowledgeBase(String userId, String kbId) {
    knowledgeBaseService.getOwnedOrThrow(userId, kbId);
    return knowledgeDocumentRepository.findByKnowledgeBaseIdOrderByCreatedAtDesc(kbId);
  }

  public KnowledgeDocument getOwnedDocument(String userId, String documentId) {
    return knowledgeDocumentRepository.findByIdAndUserId(documentId, userId)
        .orElseThrow(() -> new RuntimeException("文档不存在或无访问权限"));
  }

  @Transactional
  public KnowledgeDocument upload(String userId, String kbId, MultipartFile file) {
    KnowledgeBase kb = knowledgeBaseService.getOwnedOrThrow(userId, kbId);
    Path kbPath = Path.of(kb.getStoragePath());
    try {
      Files.createDirectories(kbPath);
    } catch (IOException e) {
      throw new RuntimeException("无法创建知识库目录: " + e.getMessage(), e);
    }

    String originalName = sanitizeFilename(file.getOriginalFilename());
    String storedFilename = buildStoredFilename(originalName);
    Path filePath = kbPath.resolve(storedFilename);

    KnowledgeDocument document = new KnowledgeDocument();
    document.setKnowledgeBaseId(kb.getId());
    document.setUserId(userId);
    document.setOriginalFilename(originalName);
    document.setStoredFilename(storedFilename);
    document.setStoragePath(filePath.toAbsolutePath().toString());
    document.setFileSize(file.getSize());
    document.setContentType(file.getContentType());
    document.setStatus(KnowledgeDocument.DocumentStatus.PARSING);

    try {
      Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new RuntimeException("保存文件失败: " + e.getMessage(), e);
    }

    document = knowledgeDocumentRepository.save(document);

    try {
      ParsedContent parsed = parseFile(filePath, originalName, file.getContentType());
      if (parsed.text() != null && !parsed.text().isBlank()) {
        Path parsedDir = kbPath.resolve("parsed");
        Files.createDirectories(parsedDir);
        Path textPath = parsedDir.resolve(storedFilename + ".txt");
        Files.writeString(textPath, parsed.text(), StandardCharsets.UTF_8);
        document.setTextPath(textPath.toAbsolutePath().toString());
        document.setSummary(buildSummary(parsed.text()));
      }
      document.setPageCount(parsed.pageCount());
      document.setStatus(KnowledgeDocument.DocumentStatus.READY);
      document.setParsedAt(LocalDateTime.now());
    } catch (Exception e) {
      document.setStatus(KnowledgeDocument.DocumentStatus.FAILED);
      document.setParseError(e.getMessage());
    }

    KnowledgeDocument saved = knowledgeDocumentRepository.save(document);

    // 将文件推送到 ai_service 侧做向量入库，成功则以 ai_service 结果为准标记 READY
    boolean aiOk = aiServiceClient.uploadDocument(userId, kbId, filePath.toFile(), originalName);
    if (aiOk && saved.getStatus() == KnowledgeDocument.DocumentStatus.FAILED) {
      saved.setStatus(KnowledgeDocument.DocumentStatus.READY);
      saved.setParseError(null);
      saved = knowledgeDocumentRepository.save(saved);
    }

    return saved;
  }

  private ParsedContent parseFile(Path filePath, String filename, String contentType) throws IOException {
    boolean isPdf = isPdf(filename, contentType);
    if (!isPdf) {
      return new ParsedContent(null, null);
    }
    try (PDDocument pdf = PDDocument.load(filePath.toFile())) {
      PDFTextStripper stripper = new PDFTextStripper();
      String text = stripper.getText(pdf);
      return new ParsedContent(text, pdf.getNumberOfPages());
    }
  }

  private String buildSummary(String text) {
    String normalized = text.trim().replaceAll("\\s+", " ");
    int len = Math.min(normalized.length(), 220);
    return normalized.substring(0, len);
  }

  private boolean isPdf(String filename, String contentType) {
    String lowerName = filename == null ? "" : filename.toLowerCase();
    String lowerType = contentType == null ? "" : contentType.toLowerCase();
    return lowerName.endsWith(".pdf") || lowerType.contains("pdf");
  }

  private String sanitizeFilename(String original) {
    if (original == null || original.isBlank()) {
      return "document.pdf";
    }
    return original.replaceAll("[\\\\/:*?\"<>|]", "_");
  }

  private String buildStoredFilename(String originalName) {
    String ext = "";
    int idx = originalName.lastIndexOf('.');
    if (idx > -1 && idx < originalName.length() - 1) {
      ext = originalName.substring(idx);
    }
    return UUID.randomUUID().toString().replace("-", "") + ext;
  }

  private record ParsedContent(String text, Integer pageCount) {
  }
}
