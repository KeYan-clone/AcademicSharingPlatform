package com.scholar.platform.repository;

import com.scholar.platform.entity.Scholar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScholarRepository extends JpaRepository<Scholar, String> {

  List<Scholar> findByPublicNameContaining(String name);

  List<Scholar> findByOrganizationContaining(String organization);
}
