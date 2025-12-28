package com.scholar.platform.repository;

import com.scholar.platform.entity.ScholarRanking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScholarRankingRepository extends JpaRepository<ScholarRanking, String> {

    /**
     * 根据领域查询并按影响力分数降序排列
     * 对应 SQL: SELECT * FROM scholar_ranking WHERE domain = ? ORDER BY influence_score DESC
     */
    //List<ScholarRanking> findByDomainOrderByInfluenceScoreDesc(String domain);
}
