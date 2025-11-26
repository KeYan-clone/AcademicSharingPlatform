package com.scholar.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@Schema(description = "分页响应")
public class PageResponse<T> {

  @Schema(description = "数据列表")
  private List<T> content;

  @Schema(description = "当前页码")
  private int pageNumber;

  @Schema(description = "每页大小")
  private int pageSize;

  @Schema(description = "总元素数")
  private long totalElements;

  @Schema(description = "总页数")
  private int totalPages;

  @Schema(description = "是否为最后一页")
  private boolean last;

  public static <T> PageResponse<T> of(Page<T> page) {
    PageResponse<T> response = new PageResponse<>();
    response.setContent(page.getContent());
    response.setPageNumber(page.getNumber());
    response.setPageSize(page.getSize());
    response.setTotalElements(page.getTotalElements());
    response.setTotalPages(page.getTotalPages());
    response.setLast(page.isLast());
    return response;
  }
}
