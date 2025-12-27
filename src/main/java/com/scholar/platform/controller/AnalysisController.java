package com.scholar.platform.controller;

import com.scholar.platform.dto.HotTopicsResponse;
import com.scholar.platform.entity.PaperKeyword;
import com.scholar.platform.repository.PaperKeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin
@RestController
@RequestMapping("/analysis")
@RequiredArgsConstructor
public class AnalysisController {
    private final PaperKeywordRepository paperKeywordRepository;

    @GetMapping("/hot-topics")
    public HotTopicsResponse getHotTopics(@RequestParam(value = "time_range", required = false) String range) {
        String r = (range == null) ? "all" : range.trim().toLowerCase();
        System.err.println("range param: [" + range + "] r: [" + r + "]");
        List<PaperKeyword> keywords = paperKeywordRepository.findAllKeywords();
        int month = LocalDate.now().getMonthValue();
        List<HotTopicsResponse.TopicWeight> result = new ArrayList<>();
        for (PaperKeyword pk : keywords) {
            int weight = 0;
            if ("1y".equals(r)) {
                weight = sumYear(pk);
            } else if ("3m".equals(r)) {
                weight = sumRecentMonths(pk, month, 3);
            } else if ("all".equals(r)) {
                weight = pk.getCnt() == null ? 0 : pk.getCnt();
            } else {
                throw new IllegalArgumentException("range参数错误");
            }
            result.add(new HotTopicsResponse.TopicWeight(pk.getKeyword(), weight));
        }
        result = result.stream()
                .filter(t -> t.getWeight() > 0)
                .sorted(Comparator.comparingInt(HotTopicsResponse.TopicWeight::getWeight).reversed())
                .collect(Collectors.toList());
        return new HotTopicsResponse(result);
    }

    private int sumYear(PaperKeyword pk) {
        int sum = 0;
        for (int i = 1; i <= 12; i++) {
            sum += getMonthCnt(pk, i);
        }
        return sum;
    }

    private int sumRecentMonths(PaperKeyword pk, int currentMonth, int range) {
        int sum = 0;
        for (int i = 0; i < range; i++) {
            int m = currentMonth - i;
            if (m <= 0) m += 12;
            sum += getMonthCnt(pk, m);
        }
        return sum;
    }

    private int getMonthCnt(PaperKeyword pk, int month) {
        try {
            java.lang.reflect.Field field = PaperKeyword.class.getDeclaredField("cnt" + month);
            field.setAccessible(true);
            Object val = field.get(pk);
            return val == null ? 0 : (int) val;
        } catch (Exception e) {
            return 0;
        }
    }
}
