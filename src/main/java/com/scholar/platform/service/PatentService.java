package com.scholar.platform.service;

import com.scholar.platform.dto.PatentDTO;
import com.scholar.platform.entity.Patent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PatentService {

    private final ElasticsearchOperations elasticsearchOperations;

    public Page<PatentDTO> searchPatents(String keyword, Integer applicationYear, Integer grantYear, Pageable pageable) {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        if (keyword != null && !keyword.trim().isEmpty()) {
            boolBuilder.must(QueryBuilders.multiMatch(m -> m
                    .fields("patentName", "abstractText", "applicant", "inventor")
                    .query(keyword)
            ));
        } else {
            boolBuilder.must(QueryBuilders.matchAll(m -> m));
        }

        if (applicationYear != null) {
            boolBuilder.filter(QueryBuilders.term(t -> t
                    .field("applicationYear")
                    .value(applicationYear)
            ));
        }

        if (grantYear != null) {
            boolBuilder.filter(QueryBuilders.term(t -> t
                    .field("grantYear")
                    .value(grantYear)
            ));
        }

        NativeQuery nativeQuery = new NativeQueryBuilder()
                .withQuery(boolBuilder.build()._toQuery())
                .withPageable(pageable)
                .build();

        SearchHits<Patent> hits = elasticsearchOperations.search(nativeQuery, Patent.class);
        List<PatentDTO> list = hits.getSearchHits().stream()
                .map(hit -> PatentDTO.fromEntity(hit.getContent()))
                .collect(Collectors.toList());

        return new PageImpl<>(list, pageable, hits.getTotalHits());
    }
}
