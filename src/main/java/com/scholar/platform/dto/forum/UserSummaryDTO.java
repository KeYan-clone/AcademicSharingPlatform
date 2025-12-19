package com.scholar.platform.dto.forum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserSummaryDTO {
    private String userId;
    private String username;
    private String avatarUrl;
    // 如果 User 表有 title (职称) 字段，也可以加上
    // private String title; 
}
