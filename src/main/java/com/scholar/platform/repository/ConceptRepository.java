package com.scholar.platform.repository;

import com.scholar.platform.entity.Concept;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ConceptRepository extends JpaRepository<Concept, String> {

    @Modifying
    @Query("UPDATE Concept c SET c.heatCount = c.heatCount + 1 WHERE c.concept = :concept")
    int incrementHeatCount(String concept);
}
