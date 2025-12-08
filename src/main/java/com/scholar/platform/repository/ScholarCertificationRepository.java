package com.scholar.platform.repository;

import com.scholar.platform.entity.ScholarCertification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScholarCertificationRepository extends JpaRepository<ScholarCertification, String> {

  List<ScholarCertification> findByUserId(String userId);

  List<ScholarCertification> findByStatus(ScholarCertification.CertificationStatus status);

  List<ScholarCertification> findByUserIdAndStatus(String userId, ScholarCertification.CertificationStatus status);

  ScholarCertification findFirstByUserIdOrderBySubmittedAtDesc(String userId);
}
