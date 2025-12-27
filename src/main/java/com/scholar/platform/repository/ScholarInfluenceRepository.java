package com.scholar.platform.repository;

import com.scholar.platform.entity.ScholarInfluence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScholarInfluenceRepository extends JpaRepository<ScholarInfluence, String> {

}
