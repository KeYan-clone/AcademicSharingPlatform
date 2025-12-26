package com.scholar.platform.util;

import com.scholar.platform.entity.Achievement;
import com.scholar.platform.entity.AchievementAuthor;
import com.scholar.platform.repository.AchievementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Utils {

    @Autowired
    private static AchievementRepository achievementRepository;


    /**
     * 根据achievementId获取achievement的简要信息
     */
    public static Achievement getAchievement(String achievementId) {
        Achievement achievement = achievementRepository.findById(achievementId).orElse(null);
        if (achievement == null) {
            return null;
        }
        return achievement;
    }

    public static Achievement getAchievement(AchievementAuthor achievementAuthor) {
        Achievement achievement = achievementRepository.findById(achievementAuthor.getAchievementId()).orElse(null);
        if (achievement == null) {
            return null;
        }
        return achievement;
    }
}
