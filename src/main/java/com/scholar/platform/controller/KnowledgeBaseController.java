package com.scholar.platform.controller;

import com.scholar.platform.dto.ApiResponse;
import com.scholar.platform.dto.CreateKnowledgeBaseRequest;
import com.scholar.platform.dto.KnowledgeBaseResponse;
import com.scholar.platform.dto.KnowledgeDocumentResponse;
import com.scholar.platform.dto.QaRequest;
import com.scholar.platform.dto.QaResponse;
import com.scholar.platform.dto.UpdateKnowledgeBaseRequest;
import com.scholar.platform.entity.KnowledgeBase;
import com.scholar.platform.entity.KnowledgeDocument;
import com.scholar.platform.entity.User;
import com.scholar.platform.service.AiServiceClient;
import com.scholar.platform.service.KnowledgeBaseService;
import com.scholar.platform.service.KnowledgeDocumentService;
import com.scholar.platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin
@RestController
@RequestMapping("/kb")
@RequiredArgsConstructor
@Tag(name = "个人智库", description = "知识库与文档管理接口")
@SecurityRequirement(name = "Bearer Authentication")
public class KnowledgeBaseController {

  private final KnowledgeBaseService knowledgeBaseService;
  private final KnowledgeDocumentService knowledgeDocumentService;
  private final UserService userService;
  private final AiServiceClient aiServiceClient;

  @PostMapping
  @Operation(summary = "创建知识库", description = "为当前用户创建新的个人知识库")
  public ResponseEntity<ApiResponse<KnowledgeBaseResponse>> createKnowledgeBase(
      @Valid @RequestBody CreateKnowledgeBaseRequest request) {
    String userId = currentUser().getId();
    KnowledgeBase kb = knowledgeBaseService.create(
        userId,
        request.getName(),
        request.getDescription(),
        request.getVisibility());
    return ResponseEntity.status(201).body(ApiResponse.success(201, "创建成功", toResponse(kb)));
  }

  @GetMapping
  @Operation(summary = "知识库列表", description = "获取当前用户的知识库列表")
  public ResponseEntity<ApiResponse<List<KnowledgeBaseResponse>>> listKnowledgeBases() {
    String userId = currentUser().getId();
    List<KnowledgeBaseResponse> list = knowledgeBaseService.listByUser(userId)
        .stream()
        .map(this::toResponse)
        .collect(Collectors.toList());
    return ResponseEntity.ok(ApiResponse.success(list));
  }

  @PutMapping("/{id}")
  @Operation(summary = "更新知识库", description = "修改知识库名称、描述或可见性")
  public ResponseEntity<ApiResponse<KnowledgeBaseResponse>> updateKnowledgeBase(
      @PathVariable String id,
      @Valid @RequestBody UpdateKnowledgeBaseRequest request) {
    String userId = currentUser().getId();
    KnowledgeBase kb = knowledgeBaseService.update(
        userId,
        id,
        request.getName(),
        request.getDescription(),
        request.getVisibility());
    return ResponseEntity.ok(ApiResponse.success("更新成功", toResponse(kb)));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "删除知识库", description = "删除知识库及其下的文档")
  public ResponseEntity<ApiResponse<Void>> deleteKnowledgeBase(@PathVariable String id) {
    String userId = currentUser().getId();
    knowledgeBaseService.delete(userId, id);
    return ResponseEntity.ok(ApiResponse.success("删除成功", null));
  }

  @GetMapping("/{id}/documents")
  @Operation(summary = "文档列表", description = "查看指定知识库下的文档解析状态")
  public ResponseEntity<ApiResponse<List<KnowledgeDocumentResponse>>> listDocuments(@PathVariable String id) {
    String userId = currentUser().getId();
    List<KnowledgeDocumentResponse> docs = knowledgeDocumentService.listByKnowledgeBase(userId, id)
        .stream()
        .map(this::toResponse)
        .collect(Collectors.toList());
    return ResponseEntity.ok(ApiResponse.success(docs));
  }

  @PostMapping(value = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(summary = "上传文档", description = "上传 PDF 到指定知识库并同步解析")
  public ResponseEntity<ApiResponse<KnowledgeDocumentResponse>> uploadDocument(
      @PathVariable String id,
      @RequestPart("file") MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new RuntimeException("上传文件不能为空");
    }
    String userId = currentUser().getId();
    KnowledgeDocument doc = knowledgeDocumentService.upload(userId, id, file);
    return ResponseEntity.status(201).body(ApiResponse.success(201, "上传成功", toResponse(doc)));
  }

  private KnowledgeBaseResponse toResponse(KnowledgeBase kb) {
    return new KnowledgeBaseResponse(
        kb.getId(),
        kb.getName(),
        kb.getDescription(),
        kb.getVisibility(),
        kb.getCreatedAt(),
        kb.getUpdatedAt()
    );
  }

  private KnowledgeDocumentResponse toResponse(KnowledgeDocument doc) {
    return new KnowledgeDocumentResponse(
        doc.getId(),
        doc.getKnowledgeBaseId(),
        doc.getOriginalFilename(),
        doc.getStatus(),
        doc.getFileSize(),
        doc.getPageCount(),
        doc.getSummary(),
        doc.getParsedAt(),
        doc.getCreatedAt(),
        doc.getUpdatedAt(),
        doc.getParseError()
    );
  }

  private User currentUser() {
    return userService.getByEmailOrThrow(currentUserEmail());
  }

  private String currentUserEmail() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || authentication.getName() == null) {
      throw new RuntimeException("未登录");
    }
    return authentication.getName();
  }

  @PostMapping("/qa")
  @Operation(summary = "知识库问答", description = "调用 ai_service 基于用户知识库进行问答，返回答案和引用")
  public ResponseEntity<ApiResponse<QaResponse>> qa(@Valid @RequestBody QaRequest request) {
    User user = currentUser();
    var result = aiServiceClient.qa(user.getId(), request.getKbId(), request.getQuestion(), request.getTopK());
    QaResponse resp = new QaResponse();
    Object ans = result.get("answer");
    resp.setAnswer(ans == null ? "" : String.valueOf(ans));
    Object refs = result.get("references");
    if (refs instanceof List<?> list) {
      //noinspection unchecked
      resp.setReferences((List<Map<String, Object>>) refs);
    }
    return ResponseEntity.ok(ApiResponse.success(resp));
  }
}
