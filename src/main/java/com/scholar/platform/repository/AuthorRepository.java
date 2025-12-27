package com.scholar.platform.repository;

import com.scholar.platform.entity.Author;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthorRepository extends ElasticsearchRepository<Author, String> {

    /**
     * 根据作者姓名精确匹配（不区分大小写）
     */
    @Query("{\"match\": {\"display_name\": {\"query\": \"?0\", \"operator\": \"and\"}}}")
    Page<Author> findByDisplayName(String displayName, Pageable pageable);

    /**
     * 模糊匹配作者姓名
     * 使用 match 查询，operator 默认为 OR，ES 会根据相关性评分排序，最匹配的在第一条
     */
    //@Query("{\"match\": {\"display_name\": {\"query\": \"?0\", \"fuzziness\": \"AUTO\"}}}")
    @Query("{\"bool\": {\"should\": [" +
           "  {\"match_phrase\": {\"display_name\": {\"query\": \"?0\", \"boost\": 2}}}, " + // 短语匹配权重更高
           "  {\"match\": {\"display_name\": {\"query\": \"?0\", \"operator\": \"and\"}}}" + // 必须包含所有词
           "], \"minimum_should_match\": 1}}")
    Page<Author> findByDisplayNameFuzzy(String displayName, Pageable pageable);
}
