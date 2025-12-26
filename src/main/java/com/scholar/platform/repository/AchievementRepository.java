package com.scholar.platform.repository;

import com.scholar.platform.entity.Achievement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AchievementRepository extends ElasticsearchRepository<Achievement, String> {

  Optional<Achievement> findByDoi(String doi);

  Page<Achievement> findByTitleContaining(String title, Pageable pageable);

  Page<Achievement> findByPublicationYear(Integer year, Pageable pageable);

  List<Achievement> findByStatus(Achievement.AchievementStatus status);
}
