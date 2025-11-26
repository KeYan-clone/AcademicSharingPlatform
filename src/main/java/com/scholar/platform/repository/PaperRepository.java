package com.scholar.platform.repository;

import com.scholar.platform.entity.Paper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaperRepository extends JpaRepository<Paper, String> {

  Optional<Paper> findByDoi(String doi);

  Page<Paper> findByTitleContaining(String title, Pageable pageable);

  Page<Paper> findByPublicationYear(Integer year, Pageable pageable);
}
