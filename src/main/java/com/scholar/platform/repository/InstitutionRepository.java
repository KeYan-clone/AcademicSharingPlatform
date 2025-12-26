package com.scholar.platform.repository;

import com.scholar.platform.entity.Institution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InstitutionRepository extends ElasticsearchRepository<Institution, String> {

    /**
     * 根据机构名称精确匹配（不区分大小写）
     * 使用 match 查询，operator 为 and 确保所有词都匹配
     */
    @Query("{\"match\": {\"display_name\": {\"query\": \"?0\", \"operator\": \"and\"}}}")
    Page<Institution> findByDisplayName(String displayName, Pageable pageable);
}
