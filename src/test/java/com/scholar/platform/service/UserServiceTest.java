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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private AchievementRepository achievementRepository;

  @Mock
  private AchievementAuthorRepository achievementAuthorRepository;

  @Mock
  private UserCollectionRepository userCollectionRepository;

  @InjectMocks
  private UserService userService;

  private User testUser;
  private Achievement testAchievement;
  private AchievementAuthor testAuthor;

  @BeforeEach
  void setUp() {
    // 初始化测试用户
    testUser = new User();
    testUser.setId("user-123");
    testUser.setUsername("测试用户");
    testUser.setEmail("test@example.com");
    testUser.setPasswordHash("hashed-password");
    testUser.setRole(User.UserRole.USER);
    testUser.setCertificationStatus(User.CertificationStatus.CERTIFIED);

    // 初始化测试成果
    testAchievement = new Achievement();
    testAchievement.setId("achievement-123");
    testAchievement.setType(Achievement.AchievementType.PAPER);
    testAchievement.setTitle("基于深度学习的文本分析");
    testAchievement.setPublicationYear(2023);
    testAchievement.setAbstractText("本文提出了一种新的方法...");
    testAchievement.setDoi("10.1109/TEST.2023.001");
    testAchievement.setPublicationVenue("IEEE Conference");
    testAchievement.setCitationCount(10);
    testAchievement.setStatus(Achievement.AchievementStatus.APPROVED);
    testAchievement.setCreatedAt(LocalDateTime.now());

    // 初始化作者关联
    testAuthor = new AchievementAuthor();
    testAuthor.setId("author-123");
    testAuthor.setAchievement(testAchievement);
    testAuthor.setAuthorUser(testUser);
    testAuthor.setAuthorName("测试用户");
    testAuthor.setAuthorOrder(1);
  }

  @Test
  void testGetUserAchievements_WithStatus() {
    // Given
    when(achievementAuthorRepository.findByAuthorUserId("user-123"))
        .thenReturn(Arrays.asList(testAuthor));

    // When
    List<AchievementDTO> achievements = userService.getUserAchievements("user-123",
        Achievement.AchievementStatus.APPROVED);

    // Then
    assertNotNull(achievements);
    assertEquals(1, achievements.size());
    assertEquals("基于深度学习的文本分析", achievements.get(0).getTitle());
    assertEquals("PAPER", achievements.get(0).getType());
    verify(achievementAuthorRepository, times(1)).findByAuthorUserId("user-123");
  }

  @Test
  void testGetUserAchievements_WithNullStatus() {
    // Given
    when(achievementAuthorRepository.findByAuthorUserId("user-123"))
        .thenReturn(Arrays.asList(testAuthor));

    // When
    List<AchievementDTO> achievements = userService.getUserAchievements("user-123", null);

    // Then
    assertNotNull(achievements);
    assertEquals(1, achievements.size());
  }

  @Test
  void testAddUserAchievement_Success() {
    // Given
    AchievementRequest request = new AchievementRequest();
    request.setType(Achievement.AchievementType.PAPER);
    request.setTitle("新论文标题");
    request.setPublicationYear(2024);
    request.setAbstractText("摘要内容");
    request.setDoi("10.1109/NEW.2024.001");

    when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
    when(achievementRepository.save(any(Achievement.class))).thenReturn(testAchievement);
    when(achievementAuthorRepository.save(any(AchievementAuthor.class))).thenReturn(testAuthor);

    // When
    AchievementDTO result = userService.addUserAchievement("user-123", request);

    // Then
    assertNotNull(result);
    assertEquals("基于深度学习的文本分析", result.getTitle());
    verify(userRepository, times(1)).findById("user-123");
    verify(achievementRepository, times(1)).save(any(Achievement.class));
    verify(achievementAuthorRepository, times(1)).save(any(AchievementAuthor.class));
  }

  @Test
  void testAddUserAchievement_UserNotFound() {
    // Given
    AchievementRequest request = new AchievementRequest();
    request.setType(Achievement.AchievementType.PAPER);
    request.setTitle("新论文标题");

    when(userRepository.findById("user-999")).thenReturn(Optional.empty());

    // When & Then
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      userService.addUserAchievement("user-999", request);
    });
    assertEquals("用户不存在", exception.getMessage());
  }

  @Test
  void testUpdateUserAchievement_Success() {
    // Given
    AchievementRequest request = new AchievementRequest();
    request.setType(Achievement.AchievementType.PAPER);
    request.setTitle("更新后的标题");
    request.setPublicationYear(2024);

    when(achievementRepository.findById("achievement-123")).thenReturn(Optional.of(testAchievement));
    when(achievementAuthorRepository.findByAchievementId("achievement-123"))
        .thenReturn(Arrays.asList(testAuthor));
    when(achievementRepository.save(any(Achievement.class))).thenReturn(testAchievement);

    // When
    AchievementDTO result = userService.updateUserAchievement("user-123", "achievement-123", request);

    // Then
    assertNotNull(result);
    verify(achievementRepository, times(1)).save(any(Achievement.class));
  }

  @Test
  void testUpdateUserAchievement_NotAuthor() {
    // Given
    AchievementRequest request = new AchievementRequest();
    request.setType(Achievement.AchievementType.PAPER);
    request.setTitle("更新后的标题");

    when(achievementRepository.findById("achievement-123")).thenReturn(Optional.of(testAchievement));
    when(achievementAuthorRepository.findByAchievementId("achievement-123"))
        .thenReturn(Arrays.asList(testAuthor));

    // When & Then
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      userService.updateUserAchievement("user-999", "achievement-123", request);
    });
    assertEquals("无权修改该成果", exception.getMessage());
  }

  @Test
  void testDeleteUserAchievement_Success() {
    // Given
    when(achievementRepository.findById("achievement-123")).thenReturn(Optional.of(testAchievement));
    when(achievementAuthorRepository.findByAchievementId("achievement-123"))
        .thenReturn(Arrays.asList(testAuthor));

    // When
    userService.deleteUserAchievement("user-123", "achievement-123");

    // Then
    verify(achievementRepository, times(1)).delete(testAchievement);
  }

  @Test
  void testDeleteUserAchievement_NotAuthor() {
    // Given
    when(achievementRepository.findById("achievement-123")).thenReturn(Optional.of(testAchievement));
    when(achievementAuthorRepository.findByAchievementId("achievement-123"))
        .thenReturn(Arrays.asList(testAuthor));

    // When & Then
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      userService.deleteUserAchievement("user-999", "achievement-123");
    });
    assertEquals("无权删除该成果", exception.getMessage());
  }

  @Test
  void testGetUserCollections_Success() {
    // Given
    UserCollection collection = new UserCollection();
    collection.setUserId("user-123");
    collection.setAchievementId("achievement-123");
    collection.setAchievement(testAchievement);
    collection.setSavedAt(LocalDateTime.now());

    when(userCollectionRepository.findByUserId("user-123"))
        .thenReturn(Arrays.asList(collection));

    // When
    List<CollectionDTO> collections = userService.getUserCollections("user-123");

    // Then
    assertNotNull(collections);
    assertEquals(1, collections.size());
    assertEquals("基于深度学习的文本分析", collections.get(0).getTitle());
    assertEquals(Achievement.AchievementType.PAPER, collections.get(0).getType());
  }

  @Test
  void testAddUserCollection_Success() {
    // Given
    UserCollection collection = new UserCollection();
    collection.setUserId("user-123");
    collection.setAchievementId("achievement-123");
    collection.setSavedAt(LocalDateTime.now());

    when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
    when(achievementRepository.findById("achievement-123")).thenReturn(Optional.of(testAchievement));
    when(userCollectionRepository.existsByUserIdAndAchievementId("user-123", "achievement-123"))
        .thenReturn(false);
    when(userCollectionRepository.save(any(UserCollection.class))).thenReturn(collection);

    // When
    CollectionDTO result = userService.addUserCollection("user-123", "achievement-123");

    // Then
    assertNotNull(result);
    assertEquals("achievement-123", result.getAchievementId());
    assertEquals("基于深度学习的文本分析", result.getTitle());
    verify(userCollectionRepository, times(1)).save(any(UserCollection.class));
  }

  @Test
  void testAddUserCollection_AlreadyCollected() {
    // Given
    when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));
    when(achievementRepository.findById("achievement-123")).thenReturn(Optional.of(testAchievement));
    when(userCollectionRepository.existsByUserIdAndAchievementId("user-123", "achievement-123"))
        .thenReturn(true);

    // When & Then
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      userService.addUserCollection("user-123", "achievement-123");
    });
    assertEquals("已经收藏过该成果", exception.getMessage());
  }

  @Test
  void testDeleteUserCollection_Success() {
    // Given
    when(userCollectionRepository.existsById(any())).thenReturn(true);

    // When
    userService.deleteUserCollection("user-123", "achievement-123");

    // Then
    verify(userCollectionRepository, times(1)).deleteById(any());
  }

  @Test
  void testDeleteUserCollection_NotFound() {
    // Given
    when(userCollectionRepository.existsById(any())).thenReturn(false);

    // When & Then
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      userService.deleteUserCollection("user-123", "achievement-123");
    });
    assertEquals("收藏不存在", exception.getMessage());
  }
}
