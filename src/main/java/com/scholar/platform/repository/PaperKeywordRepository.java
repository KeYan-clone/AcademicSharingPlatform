package com.scholar.platform.repository;

import com.scholar.platform.entity.PaperKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaperKeywordRepository extends JpaRepository<PaperKeyword, String> {
    Optional<PaperKeyword> findByKeyword(String keyword);

    @Query("SELECT pk FROM PaperKeyword pk")
    List<PaperKeyword> findAllKeywords();

    PaperKeyword findFirstByOrderByCntDesc();
}
