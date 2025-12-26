package com.scholar.platform.service;

import com.scholar.platform.dto.ScholarDTO;
import com.scholar.platform.entity.Achievement;
import com.scholar.platform.entity.AchievementAuthor;
import com.scholar.platform.entity.Scholar;
import com.scholar.platform.entity.User;
import com.scholar.platform.repository.AchievementAuthorRepository;
import com.scholar.platform.repository.ScholarRepository;
import com.scholar.platform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScholarServiceTest {

  @Mock
  private ScholarRepository scholarRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private AchievementAuthorRepository achievementAuthorRepository;

  @InjectMocks
  private ScholarService scholarService;

  private Scholar testScholar1;
  private Scholar testScholar2;
  private User testUser1;
  private User testUser2;
  private Achievement testAchievement;

  @BeforeEach
  void setUp() {
    // 初始化测试用户1
    testUser1 = new User();
    testUser1.setId("user-1");
    testUser1.setUsername("张三");
    testUser1.setEmail("zhangsan@example.com");

    // 初始化测试用户2
    testUser2 = new User();
    testUser2.setId("user-2");
    testUser2.setUsername("李四");
    testUser2.setEmail("lisi@example.com");

    // 初始化测试学者1
    testScholar1 = new Scholar();
    testScholar1.setUserId("user-1");
    testScholar1.setUser(testUser1);
    testScholar1.setPublicName("张三教授");
    testScholar1.setOrganization("清华大学");
    testScholar1.setTitle("教授");
    testScholar1.setBio("专注于人工智能研究");
    testScholar1.setAvatarUrl("http://example.com/avatar1.jpg");

    // 初始化测试学者2
    testScholar2 = new Scholar();
    testScholar2.setUserId("user-2");
    testScholar2.setUser(testUser2);
    testScholar2.setPublicName("李四副教授");
    testScholar2.setOrganization("北京大学");
    testScholar2.setTitle("副教授");
    testScholar2.setBio("专注于机器学习研究");
    testScholar2.setAvatarUrl("http://example.com/avatar2.jpg");

    // 初始化测试成果
    testAchievement = new Achievement();
    testAchievement.setId("achievement-1");
    testAchievement.setTitle("深度学习研究论文");
  }

  @Test
  void testSearchScholar_ByName() {
    // Given
    when(scholarRepository.findByPublicNameContaining("张三"))
        .thenReturn(Arrays.asList(testScholar1));

    // When
    List<ScholarDTO> results = scholarService.searchScholar(
        Optional.of("张三"), Optional.empty(), Optional.empty());

    // Then
    assertNotNull(results);
    assertEquals(1, results.size());
    assertEquals("张三教授", results.get(0).getPublicName());
    assertEquals("清华大学", results.get(0).getOrganization());
    verify(scholarRepository, times(1)).findByPublicNameContaining("张三");
  }

  @Test
  void testSearchScholar_ByOrganization() {
    // Given
    when(scholarRepository.findByOrganizationContaining("清华"))
        .thenReturn(Arrays.asList(testScholar1));

    // When
    List<ScholarDTO> results = scholarService.searchScholar(
        Optional.empty(), Optional.of("清华"), Optional.empty());

    // Then
    assertNotNull(results);
    assertEquals(1, results.size());
    assertEquals("清华大学", results.get(0).getOrganization());
    verify(scholarRepository, times(1)).findByOrganizationContaining("清华");
  }

  @Test
  void testSearchScholar_ByNameAndOrganization() {
    // Given
    when(scholarRepository.findByPublicNameContaining("张三"))
        .thenReturn(Arrays.asList(testScholar1));

    // When
    List<ScholarDTO> results = scholarService.searchScholar(
        Optional.of("张三"), Optional.of("清华"), Optional.empty());

    // Then
    assertNotNull(results);
    assertEquals(1, results.size());
    assertEquals("张三教授", results.get(0).getPublicName());
    assertTrue(results.get(0).getOrganization().contains("清华"));
  }

  @Test
  void testSearchScholar_NoConditions() {
    // Given
    when(scholarRepository.findAll())
        .thenReturn(Arrays.asList(testScholar1, testScholar2));

    // When
    List<ScholarDTO> results = scholarService.searchScholar(
        Optional.empty(), Optional.empty(), Optional.empty());

    // Then
    assertNotNull(results);
    assertEquals(2, results.size());
    verify(scholarRepository, times(1)).findAll();
  }

  @Test
  void testFindAllCollaborators_Success() {
    // Given
    AchievementAuthor author1 = new AchievementAuthor();
    author1.setAchievementId(testAchievement.getId());
    author1.setAuthorUser(testUser1);
    author1.setAuthorOrder(1);

    AchievementAuthor author2 = new AchievementAuthor();
    author2.setAchievementId(testAchievement.getId());
    author2.setAuthorUser(testUser2);
    author2.setAuthorOrder(2);

    when(achievementAuthorRepository.findByAuthorUserId("user-1"))
        .thenReturn(Arrays.asList(author1));
    when(achievementAuthorRepository.findByAchievementId("achievement-1"))
        .thenReturn(Arrays.asList(author1, author2));
    when(scholarRepository.findById("user-2"))
        .thenReturn(Optional.of(testScholar2));

    // When
    List<ScholarDTO> collaborators = scholarService.findAllCollaborators("user-1");

    // Then
    assertNotNull(collaborators);
    assertEquals(1, collaborators.size());
    assertEquals("李四副教授", collaborators.get(0).getPublicName());
    verify(achievementAuthorRepository, times(1)).findByAuthorUserId("user-1");
  }

  @Test
  void testFindAllCollaborators_NoCollaborators() {
    // Given
    AchievementAuthor author1 = new AchievementAuthor();
    author1.setAchievementId(testAchievement.getId());
    author1.setAuthorUser(testUser1);

    when(achievementAuthorRepository.findByAuthorUserId("user-1"))
        .thenReturn(Arrays.asList(author1));
    when(achievementAuthorRepository.findByAchievementId("achievement-1"))
        .thenReturn(Arrays.asList(author1)); // 只有一个作者

    // When
    List<ScholarDTO> collaborators = scholarService.findAllCollaborators("user-1");

    // Then
    assertNotNull(collaborators);
    assertEquals(0, collaborators.size());
  }

  @Test
  void testGetScholarProfile_Success() {
    // Given
    when(scholarRepository.findById("user-1"))
        .thenReturn(Optional.of(testScholar1));

    // When
    ScholarDTO result = scholarService.getScholarProfile("user-1");

    // Then
    assertNotNull(result);
    assertEquals("user-1", result.getUserId());
    assertEquals("张三教授", result.getPublicName());
    assertEquals("清华大学", result.getOrganization());
    assertEquals("教授", result.getTitle());
    assertEquals("专注于人工智能研究", result.getBio());
    verify(scholarRepository, times(1)).findById("user-1");
  }

  @Test
  void testGetScholarProfile_NotFound() {
    // Given
    when(scholarRepository.findById("user-999"))
        .thenReturn(Optional.empty());

    // When & Then
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      scholarService.getScholarProfile("user-999");
    });
    assertEquals("学者信息不存在", exception.getMessage());
  }

  @Test
  void testUpdateScholarProfile_Success() {
    // Given
    ScholarDTO updateDTO = new ScholarDTO();
    updateDTO.setPublicName("张三研究员");
    updateDTO.setOrganization("中科院");
    updateDTO.setTitle("研究员");
    updateDTO.setBio("更新后的简介");
    updateDTO.setAvatarUrl("http://example.com/new-avatar.jpg");

    when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser1));
    when(scholarRepository.findById("user-1")).thenReturn(Optional.of(testScholar1));
    when(scholarRepository.save(any(Scholar.class))).thenReturn(testScholar1);

    // When
    ScholarDTO result = scholarService.updateScholarProfile("user-1", updateDTO);

    // Then
    assertNotNull(result);
    verify(scholarRepository, times(1)).save(any(Scholar.class));
    verify(userRepository, times(1)).findById("user-1");
  }

  @Test
  void testUpdateScholarProfile_CreateNewScholar() {
    // Given
    ScholarDTO updateDTO = new ScholarDTO();
    updateDTO.setPublicName("新学者");
    updateDTO.setOrganization("新机构");

    when(userRepository.findById("user-3")).thenReturn(Optional.of(testUser1));
    when(scholarRepository.findById("user-3")).thenReturn(Optional.empty());
    when(scholarRepository.save(any(Scholar.class))).thenReturn(testScholar1);

    // When
    ScholarDTO result = scholarService.updateScholarProfile("user-3", updateDTO);

    // Then
    assertNotNull(result);
    verify(scholarRepository, times(1)).save(any(Scholar.class));
  }

  @Test
  void testUpdateScholarProfile_UserNotFound() {
    // Given
    ScholarDTO updateDTO = new ScholarDTO();
    updateDTO.setPublicName("测试");

    when(userRepository.findById("user-999")).thenReturn(Optional.empty());

    // When & Then
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      scholarService.updateScholarProfile("user-999", updateDTO);
    });
    assertEquals("用户不存在", exception.getMessage());
  }
}
