package com.scholar.platform.dto.forum;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReplyDTO {
    private String id;
    private String content;
    private UserSummaryDTO author;
    private List<String> attachments;
    private LocalDateTime createdAt;
}
