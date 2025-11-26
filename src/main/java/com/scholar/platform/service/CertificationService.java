package com.scholar.platform.service;

import com.scholar.platform.dto.CertificationRequest;
import com.scholar.platform.entity.ScholarCertification;
import com.scholar.platform.entity.User;
import com.scholar.platform.repository.ScholarCertificationRepository;
import com.scholar.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CertificationService {

  private final ScholarCertificationRepository certificationRepository;
  private final UserRepository userRepository;

  @Transactional
  public ScholarCertification submitCertification(String userId, CertificationRequest request) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("用户不存在"));

    ScholarCertification certification = new ScholarCertification();
    certification.setUser(user);
    certification.setRealName(request.getRealName());
    certification.setOrganization(request.getOrganization());
    certification.setOrgEmail(request.getOrgEmail());
    certification.setTitle(request.getTitle());
    certification.setProofMaterials(request.getProofMaterials());
    certification.setStatus(ScholarCertification.CertificationStatus.PENDING);

    certification = certificationRepository.save(certification);

    // Update user status
    user.setCertificationStatus(User.CertificationStatus.PENDING);
    userRepository.save(user);

    return certification;
  }

  public List<ScholarCertification> getPendingCertifications() {
    return certificationRepository.findByStatus(ScholarCertification.CertificationStatus.PENDING);
  }

  @Transactional
  public ScholarCertification approveCertification(String certificationId, String adminId) {
    ScholarCertification certification = certificationRepository.findById(certificationId)
        .orElseThrow(() -> new RuntimeException("认证申请不存在"));

    User admin = userRepository.findById(adminId)
        .orElseThrow(() -> new RuntimeException("管理员不存在"));

    certification.setStatus(ScholarCertification.CertificationStatus.APPROVED);
    certification.setProcessedByAdmin(admin);
    certification.setProcessedAt(LocalDateTime.now());
    certification = certificationRepository.save(certification);

    // Update user status
    User user = certification.getUser();
    user.setCertificationStatus(User.CertificationStatus.CERTIFIED);
    userRepository.save(user);

    return certification;
  }

  @Transactional
  public ScholarCertification rejectCertification(String certificationId, String adminId, String reason) {
    ScholarCertification certification = certificationRepository.findById(certificationId)
        .orElseThrow(() -> new RuntimeException("认证申请不存在"));

    User admin = userRepository.findById(adminId)
        .orElseThrow(() -> new RuntimeException("管理员不存在"));

    certification.setStatus(ScholarCertification.CertificationStatus.REJECTED);
    certification.setRejectionReason(reason);
    certification.setProcessedByAdmin(admin);
    certification.setProcessedAt(LocalDateTime.now());
    certification = certificationRepository.save(certification);

    // Update user status
    User user = certification.getUser();
    user.setCertificationStatus(User.CertificationStatus.NOT_CERTIFIED);
    userRepository.save(user);

    return certification;
  }
}
