package com.scholar.platform.service;

import com.scholar.platform.dto.AchievementDTO;
import com.scholar.platform.dto.AchievementRequest;
import com.scholar.platform.dto.CollectionDTO;
import com.scholar.platform.entity.Achievement;
import com.scholar.platform.entity.AchievementAuthor;
import com.scholar.platform.entity.User;
import com.scholar.platform.entity.UserCollection;
import com.scholar.platform.repository.AchievementAuthorRepository;
import com.scholar.platform.repository.AchievementRepository;
import com.scholar.platform.repository.UserCollectionRepository;
import com.scholar.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AchievementRepository achievementRepository;
    private final AchievementAuthorRepository achievementAuthorRepository;
    private final UserCollectionRepository userCollectionRepository;

    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public User getByEmailOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    /**
     * 获取用户成果列表，根据状态筛选
     */
    public List<AchievementDTO> getUserAchievements(String userId, Achievement.AchievementStatus status) {
        // 获取用户作为作者的所有成果
        List<AchievementAuthor> authorships = achievementAuthorRepository.findByAuthorUserId(userId);

        return authorships.stream()
                .map(AchievementAuthor::getAchievement)
                .filter(achievement -> status == null || achievement.getStatus().equals(status))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 为用户添加成果
     */
    @Transactional
    public AchievementDTO addUserAchievement(String userId, AchievementRequest request) {
        // 验证用户存在
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 创建成果
        Achievement achievement = new Achievement();
        achievement.setType(request.getType());
        achievement.setTitle(request.getTitle());
        achievement.setPublicationYear(request.getPublicationYear());
        achievement.setAbstractText(request.getAbstractText());
        achievement.setDoi(request.getDoi());
        achievement.setPublicationVenue(request.getPublicationVenue());
        achievement.setSourceData(request.getSourceData());
        achievement.setStatus(Achievement.AchievementStatus.PENDING); // 默认待审核

        achievement = achievementRepository.save(achievement);

        // 创建作者关联（用户为第一作者）
        AchievementAuthor author = new AchievementAuthor();
        author.setAchievement(achievement);
        author.setAuthorUser(user);
        author.setAuthorName(user.getUsername());
        author.setAuthorOrder(1);
        achievementAuthorRepository.save(author);

        return toDTO(achievement);
    }

    /**
     * 更新用户成果
     */
    @Transactional
    public AchievementDTO updateUserAchievement(String userId, String achievementId, AchievementRequest request) {
        // 验证成果存在
        Achievement achievement = achievementRepository.findById(achievementId)
                .orElseThrow(() -> new RuntimeException("成果不存在"));

        // 验证用户是该成果的作者
        boolean isAuthor = achievementAuthorRepository.findByAchievementId(achievementId)
                .stream()
                .anyMatch(author -> userId
                        .equals(author.getAuthorUser() != null ? author.getAuthorUser().getId() : null));

        if (!isAuthor) {
            throw new RuntimeException("无权修改该成果");
        }

        // 更新成果信息
        achievement.setType(request.getType());
        achievement.setTitle(request.getTitle());
        achievement.setPublicationYear(request.getPublicationYear());
        achievement.setAbstractText(request.getAbstractText());
        achievement.setDoi(request.getDoi());
        achievement.setPublicationVenue(request.getPublicationVenue());
        achievement.setSourceData(request.getSourceData());

        achievement = achievementRepository.save(achievement);
        return toDTO(achievement);
    }

    /**
     * 删除用户成果
     */
    @Transactional
    public void deleteUserAchievement(String userId, String achievementId) {
        // 验证成果存在
        Achievement achievement = achievementRepository.findById(achievementId)
                .orElseThrow(() -> new RuntimeException("成果不存在"));

        // 验证用户是该成果的作者
        boolean isAuthor = achievementAuthorRepository.findByAchievementId(achievementId)
                .stream()
                .anyMatch(author -> userId
                        .equals(author.getAuthorUser() != null ? author.getAuthorUser().getId() : null));

        if (!isAuthor) {
            throw new RuntimeException("无权删除该成果");
        }

        // 删除成果（级联删除作者关联）
        achievementRepository.delete(achievement);
    }

    /**
     * 获取用户收藏的成果列表
     */
    public List<CollectionDTO> getUserCollections(String userId) {
        List<UserCollection> collections = userCollectionRepository.findByUserId(userId);

        return collections.stream()
                .map(CollectionDTO::from)
                .collect(Collectors.toList());
    }

    /**
     * 添加收藏成果
     */
    @Transactional
    public CollectionDTO addUserCollection(String userId, String achievementId) {
        // 验证用户存在
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 验证成果存在
        Achievement achievement = achievementRepository.findById(achievementId)
                .orElseThrow(() -> new RuntimeException("成果不存在"));

        // 检查是否已收藏
        if (userCollectionRepository.existsByUserIdAndAchievementId(userId, achievementId)) {
            throw new RuntimeException("已经收藏过该成果");
        }

        // 创建收藏
        UserCollection collection = new UserCollection();
        collection.setUserId(userId);
        collection.setAchievementId(achievementId);
        collection = userCollectionRepository.save(collection);

        // 转换为DTO
        CollectionDTO dto = new CollectionDTO();
        dto.setAchievementId(achievementId);
        dto.setTitle(achievement.getTitle());
        dto.setType(achievement.getType());
        dto.setSavedAt(collection.getSavedAt());

        return dto;
    }

    /**
     * 删除收藏成果
     */
    @Transactional
    public void deleteUserCollection(String userId, String achievementId) {
        UserCollection.UserCollectionId id = new UserCollection.UserCollectionId(userId, achievementId);

        if (!userCollectionRepository.existsById(id)) {
            throw new RuntimeException("收藏不存在");
        }

        userCollectionRepository.deleteById(id);
    }

    /**
     * 将Achievement实体转换为DTO
     */
    private AchievementDTO toDTO(Achievement achievement) {
        AchievementDTO dto = new AchievementDTO();
        dto.setId(achievement.getId());
        dto.setType(achievement.getType().name());
        dto.setTitle(achievement.getTitle());
        dto.setPublicationYear(achievement.getPublicationYear());
        dto.setAbstractText(achievement.getAbstractText());
        dto.setDoi(achievement.getDoi());
        dto.setPublicationVenue(achievement.getPublicationVenue());
        dto.setCitationCount(achievement.getCitationCount());
        dto.setCreatedAt(achievement.getCreatedAt());
        return dto;
    }
}
