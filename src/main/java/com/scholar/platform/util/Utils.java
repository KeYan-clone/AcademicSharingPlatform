package com.scholar.platform.util;

import com.scholar.platform.entity.Achievement;
import com.scholar.platform.entity.AchievementAuthor;
import com.scholar.platform.repository.AchievementRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Utils {

    @Autowired
    private AchievementRepository repository;

    private static AchievementRepository achievementRepository;

    @PostConstruct
    public void init() {
        achievementRepository = repository;
    }

    public static Achievement getAchievement(String achievementId) {
        return achievementRepository.findById(IdPrefixUtil.ensureIdPrefix(achievementId)).orElse(null);
    }

    public  static Achievement getAchievement(AchievementAuthor achievementAuthor) {
        Achievement achievement = achievementRepository.findById(IdPrefixUtil.ensureIdPrefix(achievementAuthor.getAchievementId())).orElse(null);
        if (achievement == null) {
            return null;
        }
        return achievement;
    }
}
