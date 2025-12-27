package com.scholar.platform.service;

import com.scholar.platform.dto.AchievementDTO;
import com.scholar.platform.dto.AchievementRequest;
import com.scholar.platform.dto.CollectionDTO;
import com.scholar.platform.dto.PendingClaimRequestDTO;
import com.scholar.platform.dto.UserClaimRequestDTO;
import com.scholar.platform.entity.Achievement;
import com.scholar.platform.entity.AchievementAuthor;
import com.scholar.platform.entity.AchievementClaimRequest;
import com.scholar.platform.entity.User;
import com.scholar.platform.entity.UserCollection;
import com.scholar.platform.repository.AchievementAuthorRepository;
import com.scholar.platform.repository.AchievementClaimRequestRepository;
import com.scholar.platform.repository.AchievementRepository;
import com.scholar.platform.repository.UserCollectionRepository;
import com.scholar.platform.repository.UserRepository;
import com.scholar.platform.util.Utils;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.scholar.platform.util.IdPrefixUtil;

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
    private final AchievementClaimRequestRepository achievementClaimRequestRepository;

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
                .map(Utils::getAchievement)
                .filter(achievement -> status == null || achievement.getStatus().equals(status))
                .map(Achievement::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void replyClaimAchievement(String requestId, Boolean approve, String message) {
        // 查找认证请求
        AchievementClaimRequest request = achievementClaimRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("认证请求不存在"));

        // 检查请求状态
        if (request.getStatus() != AchievementClaimRequest.ClaimStatus.PENDING) {
            throw new RuntimeException("该请求已处理，不能重复处理");
        }

        if (approve) {
            // 检查该成果对应作者序的作者是否已存在
            boolean authorExists = achievementAuthorRepository.existsByAchievementIdAndAuthorOrder(
                    request.getAchievementId(), request.getAuthorOrder());
            if (authorExists) {
                throw new RuntimeException("该成果对应作者序的作者已存在");
            }

            // 验证用户存在
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            // 验证成果存在
            Achievement achievement = achievementRepository.findById(IdPrefixUtil.ensureIdPrefix(request.getAchievementId()))
                    .orElseThrow(() -> new RuntimeException("成果不存在"));

            // 建立用户与成果的作者关联
            AchievementAuthor author = new AchievementAuthor();
            author.setAchievementId(request.getAchievementId());
            author.setAuthorUser(user);
            author.setAuthorName(user.getUsername());
            author.setAuthorOrder(request.getAuthorOrder());
            achievementAuthorRepository.save(author);

            // 将请求状态修改为成功
            request.setStatus(AchievementClaimRequest.ClaimStatus.APPROVED);
        } else {
            // 拒绝请求
            request.setStatus(AchievementClaimRequest.ClaimStatus.REJECTED);
        }
        achievementClaimRequestRepository.save(request);
    }

    /**
     * 发起成果认领请求
     */
    @Transactional
    public void requestClaimAchievement(String userId, String achievementId, Integer authorOrder) {
        // 验证用户存在
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        System.out.println(achievementId);
        // 验证成果存在
        achievementRepository.findById(IdPrefixUtil.ensureIdPrefix(achievementId))
                .orElseThrow(() -> new RuntimeException("成果不存在"));

        // 检查是否有正在进行的待审核请求
        boolean hasPendingRequest = achievementClaimRequestRepository
                .findByUserIdAndAchievementIdAndStatus(userId, achievementId,
                        AchievementClaimRequest.ClaimStatus.PENDING)
                .isPresent();

        if (hasPendingRequest) {
            throw new RuntimeException("已存在待审核的认领请求，请勿重复提交");
        }

        // 创建认领请求
        AchievementClaimRequest request = new AchievementClaimRequest();
        request.setUserId(userId);
        request.setAchievementId(achievementId);
        request.setAuthorOrder(authorOrder);
        request.setStatus(AchievementClaimRequest.ClaimStatus.PENDING);

        achievementClaimRequestRepository.save(request);
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
        achievement.setTitle(request.getTitle());
        achievement.setAbstractText(request.getAbstractText());
        achievement.setDoi(request.getDoi());

        achievement = achievementRepository.save(achievement);
        return Achievement.toDTO(achievement);
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
     * 获取用户所有的认领请求
     */
    public List<UserClaimRequestDTO> getUserClaimRequests(String userId) {
        // 获取用户的所有认领请求
        List<AchievementClaimRequest> requests = achievementClaimRequestRepository.findByUserId(userId);
        
        // 转换为DTO并添加成果标题
        return requests.stream()
                .map(request -> {
                    UserClaimRequestDTO dto = new UserClaimRequestDTO();
                    dto.setRequestId(request.getId());
                    dto.setAchievementId(request.getAchievementId());
                    dto.setAuthorOrder(request.getAuthorOrder());
                    dto.setStatus(request.getStatus());
                    dto.setCreatedAt(request.getCreatedAt());
                    dto.setUpdatedAt(request.getUpdatedAt());
                    dto.setMessage(request.getMessage());
                    
                    // 获取成果标题
                    Achievement achievement = achievementRepository.findById(IdPrefixUtil.ensureIdPrefix(request.getAchievementId()))
                            .orElseThrow(() -> new RuntimeException("成果不存在"));
                    dto.setAchievementTitle(achievement.getTitle());
                    
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取所有待审核的认领请求
     */
    public List<PendingClaimRequestDTO> getPendingClaimRequests() {
        // 获取所有待审核的认领请求
        List<AchievementClaimRequest> requests = achievementClaimRequestRepository.findByStatus(AchievementClaimRequest.ClaimStatus.PENDING);
        
        // 转换为DTO并添加用户和成果信息，忽略不存在的用户或成果
        return requests.stream()
                .map(request -> {
                    PendingClaimRequestDTO dto = new PendingClaimRequestDTO();
                    dto.setRequestId(request.getId());
                    dto.setUserId(request.getUserId());
                    dto.setAchievementId(request.getAchievementId());
                    dto.setAuthorOrder(request.getAuthorOrder());
                    dto.setMessage(request.getMessage());
                    dto.setStatus(request.getStatus());
                    dto.setCreatedAt(request.getCreatedAt());
                    
                    // 获取用户名，如果用户不存在则使用"未知用户"
                    userRepository.findById(request.getUserId())
                            .ifPresentOrElse(
                                user -> dto.setUsername(user.getUsername()),
                                () -> dto.setUsername("未知用户")
                            );
                    
                    // 获取成果标题，如果成果不存在则使用"未知成果"
                    Achievement achievement = achievementRepository.findById(IdPrefixUtil.ensureIdPrefix(request.getAchievementId()))
                            .orElse(null);
                    dto.setAchievementTitle(achievement != null ? achievement.getTitle() : "未知成果");
                    
                    return dto;
                })
                .collect(Collectors.toList());
    }

}