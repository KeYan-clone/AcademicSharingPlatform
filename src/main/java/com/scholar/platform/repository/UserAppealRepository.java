package com.scholar.platform.repository;

import com.scholar.platform.entity.UserAppeal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAppealRepository extends JpaRepository<UserAppeal, String> {

  List<UserAppeal> findByApplicantId(String applicantId);

  List<UserAppeal> findByStatus(UserAppeal.AppealStatus status);

  List<UserAppeal> findByAppealType(UserAppeal.AppealType appealType);
}
