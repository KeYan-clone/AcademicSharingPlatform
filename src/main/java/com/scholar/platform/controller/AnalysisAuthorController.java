package com.scholar.platform.controller;

import com.scholar.platform.dto.AuthorInfluenceDTO;
import com.scholar.platform.dto.ScholarRankingDTO;
import com.scholar.platform.service.AnalysisAuthorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/analysis")
@RequiredArgsConstructor
public class AnalysisAuthorController {

    private final AnalysisAuthorService analysisAuthorService;

    /**
     * 获取学者的影响力趋势/概况
     * 对应接口: GET /analysis/influence/trend/{userId}
     */
    @GetMapping("/influence/trend/{userId}")
    public ResponseEntity<AuthorInfluenceDTO> getInfluenceTrend(@PathVariable String userId) {
        AuthorInfluenceDTO data = analysisAuthorService.getAuthorTrend(userId);
        return ResponseEntity.ok(data);
    }

    /**
     * 获取领域影响力排名
     * 接口: GET /analysis/influence/ranking?domain=Computer Science
     */
    @GetMapping("/influence/ranking")
    public ResponseEntity<List<ScholarRankingDTO>> getRanking(@RequestParam String domain) {
        return ResponseEntity.ok(analysisAuthorService.getScholarRanking(domain));
    }
}
