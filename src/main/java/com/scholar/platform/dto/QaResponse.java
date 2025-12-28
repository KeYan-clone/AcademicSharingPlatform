package com.scholar.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QaResponse {
  private String answer;
  /**
   * 引用列表，每项包含 source/page/score 等信息（保持与 ai_service 返回一致）
   */
  private List<Map<String, Object>> references;
}
