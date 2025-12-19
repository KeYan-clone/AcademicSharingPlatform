package com.scholar.platform.dto.forum;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class CreateReplyRequest {
    @NotBlank(message = "回复内容不能为空")
    private String content;

    private List<String> attachments;
}
