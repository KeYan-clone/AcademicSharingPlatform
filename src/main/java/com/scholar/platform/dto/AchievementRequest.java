package com.scholar.platform.dto;

import com.scholar.platform.entity.Achievement.AchievementType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "添加学术成果的请求体")
public class AchievementRequest {

    // 成果类型：论文、专利、项目、奖项
    @NotNull(message = "成果类型不能为空")
    @Schema(description = "成果类型 (PAPER, PATENT, PROJECT, AWARD)", requiredMode = Schema.RequiredMode.REQUIRED)
    private AchievementType type;

    // 标题
    @NotBlank(message = "标题不能为空")
    @Schema(description = "成果标题", example = "基于深度学习的中文文本情感分析模型")
    private String title;

    // 发表年份
    @Schema(description = "发表/完成年份", example = "2023")
    private Integer publicationYear;

    // 摘要/描述
    @Schema(description = "摘要或详细描述", example = "本文提出了一种...")
    private String abstractText;

    // DOI（如果是论文）
    @Schema(description = "数字对象唯一标识符 (DOI)", example = "10.1109/LSP.2023.3320293")
    private String doi;

    // 发表刊物/地点
    @Schema(description = "发表刊物或颁奖机构", example = "IEEE Signal Processing Letters")
    private String publicationVenue;

    // 原始数据（可选的 JSON 格式补充数据）
    @Schema(description = "原始补充数据 (JSON 字符串)", example = "{\"authors\": [\"John Doe\"], \"pages\": \"10-15\"}")
    private String sourceData;

}