package com.scholar.platform.service;

import com.scholar.platform.dto.AppealRequest;
import com.scholar.platform.entity.User;
import com.scholar.platform.entity.UserAppeal;
import com.scholar.platform.repository.UserAppealRepository;
import com.scholar.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppealService {

  private final UserAppealRepository userAppealRepository;
  private final UserRepository userRepository;

  @Transactional
  public UserAppeal createAppeal(String applicantId, AppealRequest request) {
    User applicant = userRepository.findById(applicantId)
        .orElseThrow(() -> new RuntimeException("用户不存在"));

    UserAppeal appeal = new UserAppeal();
    appeal.setApplicant(applicant);
    appeal.setAppealType(parseAppealType(request.getAppealType()));
    appeal.setTargetId(request.getTargetId());
    appeal.setReason(request.getReason());
    appeal.setEvidenceMaterials(request.getEvidenceMaterials());
    appeal.setStatus(UserAppeal.AppealStatus.PENDING);

    return userAppealRepository.save(appeal);
  }

  public List<UserAppeal> getPendingAppeals() {
    return userAppealRepository.findByStatus(UserAppeal.AppealStatus.PENDING);
  }

  @Transactional
  public UserAppeal processAppeal(String appealId, String adminId, String action, String reason) {
    UserAppeal appeal = userAppealRepository.findById(appealId)
        .orElseThrow(() -> new RuntimeException("申诉不存在"));

    User admin = userRepository.findById(adminId)
        .orElseThrow(() -> new RuntimeException("管理员不存在"));

    if ("approve".equalsIgnoreCase(action)) {
      appeal.setStatus(UserAppeal.AppealStatus.APPROVED);
    } else if ("reject".equalsIgnoreCase(action)) {
      appeal.setStatus(UserAppeal.AppealStatus.REJECTED);
    } else {
      throw new RuntimeException("无效的操作类型");
    }

    appeal.setProcessedByAdmin(admin);
    appeal.setProcessedAt(LocalDateTime.now());

    return userAppealRepository.save(appeal);
  }

  private UserAppeal.AppealType parseAppealType(String type) {
    try {
      return UserAppeal.AppealType.valueOf(type.toUpperCase());
    } catch (Exception ex) {
      throw new RuntimeException("申诉类型不合法");
    }
  }
}
