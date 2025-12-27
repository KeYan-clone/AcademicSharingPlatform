package com.scholar.platform.service;

import com.scholar.platform.dto.CertificationRequest;
import com.scholar.platform.entity.Scholar;
import com.scholar.platform.entity.ScholarCertification;
import com.scholar.platform.entity.User;
import com.scholar.platform.repository.ScholarCertificationRepository;
import com.scholar.platform.repository.ScholarRepository;
import com.scholar.platform.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;
    private final ScholarRepository scholarRepository;

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
        certification.setProofMaterials(serializeProofMaterials(request));
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

    public ScholarCertification getLatestByUser(String userId) {
        return certificationRepository.findFirstByUserIdOrderBySubmittedAtDesc(userId);
    }

    private String serializeProofMaterials(CertificationRequest request) {
        if (request.getProofMaterials() == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(request.getProofMaterials());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("证明材料格式不正确");
        }
    }

    @Transactional
    public ScholarCertification approveCertification(String certificationId, String adminEmail) {
        ScholarCertification certification = certificationRepository.findById(certificationId)
                .orElseThrow(() -> new RuntimeException("认证申请不存在"));

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("管理员不存在"));

        certification.setStatus(ScholarCertification.CertificationStatus.APPROVED);
        certification.setProcessedByAdmin(admin);
        certification.setProcessedAt(LocalDateTime.now());
        certification = certificationRepository.save(certification);

        User user = certification.getUser();
        user.setCertificationStatus(User.CertificationStatus.CERTIFIED);
        userRepository.save(user);

        Scholar scholar = new Scholar();
        scholar.setUserId(user.getId());
        scholar.setUser(certification.getUser());
        scholar.setPublicName(certification.getRealName());
        scholar.setOrganization(certification.getOrganization());
        scholar.setTitle(certification.getTitle());
        scholarRepository.save(scholar);

        // Update user status


        return certification;
    }

    @Transactional
    public ScholarCertification rejectCertification(String certificationId, String adminEmail, String reason) {
        ScholarCertification certification = certificationRepository.findById(certificationId)
                .orElseThrow(() -> new RuntimeException("认证申请不存在"));

        User admin = userRepository.findByEmail(adminEmail)
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
