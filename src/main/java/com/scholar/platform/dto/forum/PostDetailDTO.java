package com.scholar.platform.dto.forum;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostDetailDTO {
    private String postId;
    private String title;
    private String content;
    private UserSummaryDTO author;
    private String boardId;
    private String boardName;
    private List<String> attachments;
    private Integer viewCount;
    private LocalDateTime createdAt;
    
    // 回复列表
    private List<ReplyDTO> replies;
}