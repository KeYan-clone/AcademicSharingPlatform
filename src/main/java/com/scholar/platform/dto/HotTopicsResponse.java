package com.scholar.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotTopicsResponse {
    private List<TopicWeight> topics;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopicWeight {
        private String topic;
        private int weight;
    }
}
