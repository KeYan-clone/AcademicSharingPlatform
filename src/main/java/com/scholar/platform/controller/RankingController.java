package com.scholar.platform.controller;

import com.scholar.platform.service.ScholarRankingBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rankingjobs")
@RequiredArgsConstructor
public class RankingController {

    private final ScholarRankingBatchService scholarRankingBatchService;

    /**
     * 触发分领域排行榜生成任务
     * POST /admin/jobs/generate-rankings
     */
    @PostMapping("/generate-rankings")
    public ResponseEntity<String> generateRankings() {
        String result = scholarRankingBatchService.generateDomainRankings();
        return ResponseEntity.ok(result);
    }
}
