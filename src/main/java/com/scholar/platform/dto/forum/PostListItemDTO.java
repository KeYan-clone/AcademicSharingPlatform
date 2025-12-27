package com.scholar.platform.dto.forum;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PostListItemDTO {
    private String postId;
    private String title;
    private String contentPreview; // 列表页可能只需要显示前几十个字
    private UserSummaryDTO author;
    private String boardId;
    private Integer viewCount;
    
    // 统计数据
    private Long replyCount;
    private LocalDateTime lastReplyTime;
    private LocalDateTime createdAt;
}
