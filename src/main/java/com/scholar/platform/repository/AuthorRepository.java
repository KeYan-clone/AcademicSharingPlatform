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
}
