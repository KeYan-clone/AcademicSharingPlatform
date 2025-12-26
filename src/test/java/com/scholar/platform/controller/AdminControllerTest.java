package com.scholar.platform.controller;

import com.scholar.platform.dto.AchievementDTO;
import com.scholar.platform.entity.Achievement;
import com.scholar.platform.entity.ScholarCertification;
import com.scholar.platform.entity.User;
import com.scholar.platform.entity.UserAppeal;
import com.scholar.platform.service.AchievementService;
import com.scholar.platform.service.AppealService;
import com.scholar.platform.service.CertificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

  @Mock
  private CertificationService certificationService;

  @Mock
  private AppealService appealService;

  @Mock
  private AchievementService achievementService;

  @Mock
  private Authentication authentication;

  @InjectMocks
  private AdminController adminController;

  private MockMvc mockMvc;

  private User testAdmin;
  private User testUser;
  private ScholarCertification testCertification;
  private UserAppeal testAppeal;
  private Achievement testAchievement;
  private AchievementDTO testAchievementDTO;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(adminController).build();

    // 初始化测试管理员
    testAdmin = new User();
    testAdmin.setId("admin-123");
    testAdmin.setUsername("管理员");
    testAdmin.setRole(User.UserRole.ADMIN);

    // 初始化测试用户
    testUser = new User();
    testUser.setId("user-123");
    testUser.setUsername("测试用户");
    testUser.setEmail("test@example.com");
    testUser.setCertificationStatus(User.CertificationStatus.PENDING);

    // 初始化测试认证申请
    testCertification = new ScholarCertification();
    testCertification.setId("cert-123");
    testCertification.setUser(testUser);
    testCertification.setRealName("张三");
    testCertification.setOrganization("清华大学");
    testCertification.setOrgEmail("zhangsan@tsinghua.edu.cn");
    testCertification.setTitle("副教授");
    testCertification.setStatus(ScholarCertification.CertificationStatus.PENDING);
    testCertification.setSubmittedAt(LocalDateTime.now());

    // 初始化测试申诉
    testAppeal = new UserAppeal();
    testAppeal.setId("appeal-123");
    testAppeal.setApplicant(testUser);
    testAppeal.setAppealType(UserAppeal.AppealType.IDENTITY_STOLEN);
    testAppeal.setTargetId("target-123");
    testAppeal.setReason("身份被盗用");
    testAppeal.setStatus(UserAppeal.AppealStatus.PENDING);

    // 初始化测试成果
    testAchievement = new Achievement();
    testAchievement.setId("achievement-123");
    testAchievement.setType(Achievement.AchievementType.PAPER);
    testAchievement.setTitle("深度学习研究");
    testAchievement.setPublicationYear(2023);
    testAchievement.setStatus(Achievement.AchievementStatus.PENDING);
    testAchievement.setCreatedAt(LocalDateTime.now());

    // 初始化测试成果DTO
    testAchievementDTO = new AchievementDTO();
    testAchievementDTO.setId("achievement-123");
    testAchievementDTO.setType("PAPER");
    testAchievementDTO.setTitle("深度学习研究");
    testAchievementDTO.setPublicationYear(2023);
    testAchievementDTO.setCreatedAt(LocalDateTime.now());

    lenient().when(authentication.getName()).thenReturn("admin-123");
  }

  @Test
  void testGetCertifications_WithPendingStatus() throws Exception {
    List<ScholarCertification> certifications = Arrays.asList(testCertification);
    when(certificationService.getPendingCertifications()).thenReturn(certifications);

    mockMvc.perform(get("/api/admin/certifications")
        .param("status", "pending")
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data.applications").isArray());

    verify(certificationService, times(1)).getPendingCertifications();
  }

  @Test
  void testGetCertifications_DefaultStatus() throws Exception {
    List<ScholarCertification> certifications = Arrays.asList(testCertification);
    when(certificationService.getPendingCertifications()).thenReturn(certifications);

    mockMvc.perform(get("/api/admin/certifications")
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data.applications").isArray());

    verify(certificationService, times(1)).getPendingCertifications();
  }

  @Test
  void testApproveCertification() throws Exception {
    when(certificationService.approveCertification(anyString(), anyString())).thenReturn(testCertification);

    mockMvc.perform(post("/api/admin/certifications/cert-123/approve")
        .principal(authentication)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data.message").value("认证已批准"));

    verify(certificationService, times(1)).approveCertification("cert-123", "admin-123");
  }

  @Test
  void testRejectCertification() throws Exception {
    when(certificationService.rejectCertification(anyString(), anyString(), anyString())).thenReturn(testCertification);

    String requestBody = "{\"reason\":\"材料不全\"}";

    mockMvc.perform(post("/api/admin/certifications/cert-123/reject")
        .principal(authentication)
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data.message").value("认证已驳回"));

    verify(certificationService, times(1)).rejectCertification("cert-123", "admin-123", "材料不全");
  }

  @Test
  void testGetAppeals_WithPendingStatus() throws Exception {
    List<UserAppeal> appeals = Arrays.asList(testAppeal);
    when(appealService.getPendingAppeals()).thenReturn(appeals);

    mockMvc.perform(get("/api/admin/appeals")
        .param("status", "pending")
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data.appeals").isArray());

    verify(appealService, times(1)).getPendingAppeals();
  }

  @Test
  void testGetAppeals_DefaultStatus() throws Exception {
    List<UserAppeal> appeals = Arrays.asList(testAppeal);
    when(appealService.getPendingAppeals()).thenReturn(appeals);

    mockMvc.perform(get("/api/admin/appeals")
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data.appeals").isArray());

    verify(appealService, times(1)).getPendingAppeals();
  }

  @Test
  void testProcessAppeal_Approve() throws Exception {
    when(appealService.processAppeal(anyString(), anyString(), anyString(), anyString())).thenReturn(testAppeal);

    String requestBody = "{\"action\":\"approve\",\"reason\":\"申诉合理\"}";

    mockMvc.perform(post("/api/admin/appeals/appeal-123/process")
        .principal(authentication)
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data.message").value("申诉已处理"));

    verify(appealService, times(1)).processAppeal("appeal-123", "admin-123", "approve", "申诉合理");
  }

  @Test
  void testProcessAppeal_Reject() throws Exception {
    when(appealService.processAppeal(anyString(), anyString(), anyString(), anyString())).thenReturn(testAppeal);

    String requestBody = "{\"action\":\"reject\",\"reason\":\"证据不足\"}";

    mockMvc.perform(post("/api/admin/appeals/appeal-123/process")
        .principal(authentication)
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data.message").value("申诉已处理"));

    verify(appealService, times(1)).processAppeal("appeal-123", "admin-123", "reject", "证据不足");
  }

  @Test
  void testGetPendingAchievements() throws Exception {
    List<AchievementDTO> achievements = Arrays.asList(testAchievementDTO);
    when(achievementService.getPendingAchievements()).thenReturn(achievements);

    mockMvc.perform(get("/api/admin/achievements/pending")
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data.pendingAchievements").isArray());

    verify(achievementService, times(1)).getPendingAchievements();
  }

  @Test
  void testApproveAchievement() throws Exception {
    when(achievementService.approveAchievement(anyString(), anyString())).thenReturn(testAchievement);

    mockMvc.perform(post("/api/admin/achievements/achievement-123/approve")
        .principal(authentication)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data.message").value("成果已批准"));

    verify(achievementService, times(1)).approveAchievement("achievement-123", "admin-123");
  }

  @Test
  void testRejectAchievement() throws Exception {
    when(achievementService.rejectAchievement(anyString(), anyString(), anyString())).thenReturn(testAchievement);

    String requestBody = "{\"reason\":\"内容不符合要求\"}";

    mockMvc.perform(post("/api/admin/achievements/achievement-123/reject")
        .principal(authentication)
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data.message").value("成果已驳回"));

    verify(achievementService, times(1)).rejectAchievement("achievement-123", "admin-123", "内容不符合要求");
  }
}
