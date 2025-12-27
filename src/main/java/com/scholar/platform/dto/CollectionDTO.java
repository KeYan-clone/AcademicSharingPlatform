package com.scholar.platform.dto;

import com.scholar.platform.entity.Achievement;
import com.scholar.platform.entity.UserCollection;
import com.scholar.platform.util.Utils;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "用户收藏列表中的单个项目DTO")
public class CollectionDTO {

    // --- 成果 (Achievement) 摘要信息 ---

    @Schema(description = "被收藏成果的唯一标识符", example = "a1b2c3d4-e5f6...")
    private String achievementId;

    @Schema(description = "成果标题", example = "AI与金融风险管理")
    private String title;

    @Schema(description = "收藏发生的时间")
    private LocalDateTime savedAt;

    @Schema(description = "整个成果")
    private Achievement achievement;

    /**
     * 从UserCollection实体创建DTO
     */
    public static CollectionDTO from(UserCollection collection) {
        CollectionDTO dto = new CollectionDTO();
        dto.setAchievementId(collection.getAchievementId());
        dto.setSavedAt(collection.getSavedAt());

        Achievement achievement = Utils.getAchievement(collection.getAchievementId());
        if (achievement != null) {
            dto.setTitle(achievement.getTitle());
            dto.setAchievement(achievement);
        }

        return dto;
    }
}
