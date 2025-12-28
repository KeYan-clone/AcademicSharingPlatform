package com.scholar.platform.service;

import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import com.scholar.platform.entity.Author;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchHitsIterator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScholarRankingBatchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final JdbcTemplate jdbcTemplate; // 用于执行动态 SQL

    /**
     * 核心任务：按领域生成排行榜表
     */
    @Transactional
    public String generateDomainRankings() {
        log.info("开始生成分领域学者排行榜...");
        long startTime = System.currentTimeMillis();

        log.info("正在处理总榜 (All Domains)...");
        processGlobalRanking();

        // 直接使用 fields.md 中的领域列表
        String[] domains = {
            "Medicine",
            "Social Sciences",
            "Engineering",
            "Arts and Humanities",
            "Physics and Astronomy",
            "Computer Science",
            "Agricultural and Biological Sciences",
            "Biochemistry, Genetics and Molecular Biology",
            "Environmental Science",
            "Economics, Econometrics and Finance",
            "Materials Science",
            "Business, Management and Accounting",
            "Psychology",
            "Earth and Planetary Sciences",
            "Health Professions",
            "Chemistry",
            "Mathematics",
            "Neuroscience",
            "Decision Sciences",
            "Immunology and Microbiology",
            "Energy",
            "Nursing",
            "Pharmacology, Toxicology and Pharmaceutics",
            "Dentistry",
            "Chemical Engineering",
            "Veterinary"
        };

        int processedDomains = 0;

        // 2. 遍历每个领域，处理 Top 100
        for (String domainName : domains) {
            log.info("正在处理领域: {}", domainName);
            processSingleDomain(domainName);
            processedDomains++;
        }

        long endTime = System.currentTimeMillis();
        return String.format("处理完成。共处理 %d 个领域，耗时 %d ms", processedDomains, (endTime - startTime));
    }

    private void processGlobalRanking() {
        // 1. 全局查询，不限领域，按引用量降序
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.matchAll(m -> m)) 
                .withSort(Sort.by(Sort.Direction.DESC, "cited_by_count"))
                .withPageable(PageRequest.of(0, 200)) // 取前200作为候选池 
                .build();

        SearchHits<Author> hits = elasticsearchOperations.search(query, Author.class);
        
        if (hits.getTotalHits() == 0) return;

        // 2. 转换为待插入数据
        List<Object[]> batchArgs = new ArrayList<>();
        for (SearchHit<Author> hit : hits) {
            Author author = hit.getContent();
            double score = calculateInfluenceScore(author);
            int cited = author.getCitedByCount() != null ? author.getCitedByCount() : 0;
            String tags = author.getField() != null ? String.join(", ", author.getField()) : "";
            if (tags.length() > 255) tags = tags.substring(0, 255);

            int hIndex = (author.getSummaryStats() != null && author.getSummaryStats().getHIndex() != null) ? author.getSummaryStats().getHIndex() : 0;
            int i10Index = (author.getSummaryStats() != null && author.getSummaryStats().getI10Index() != null) ? author.getSummaryStats().getI10Index() : 0;

            batchArgs.add(new Object[]{
                    author.getId(),
                    author.getDisplayName(),
                    tags,
                    hIndex,
                    i10Index,
                    author.getWorksCount(),
                    score,
                    cited
            });
        }

        // 3. 在内存中按 score 再次排序（因为 ES 是按引用量排的，score 包含其他因子）
        batchArgs.sort((o1, o2) -> Double.compare((Double) o2[6], (Double) o1[6])); // index 6 is score

        // 4. 截取前 100
        if (batchArgs.size() > 100) {
            batchArgs = batchArgs.subList(0, 100);
        }

        // 5. 存入 scholar_ranking_all 表
        String tableName = "scholar_ranking_all";
        createTableIfNotExists(tableName);
        saveToDatabase(tableName, batchArgs);
    }

    private void processSingleDomain(String domainName) {
        // 2.1 查询该领域引用量最高的 Top 100
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.match(m -> m.field("field").query(domainName))) // 筛选 fields
                //.withQuery(q -> q.term(t -> t.field("field").value(domainName))) 
                .withSort(Sort.by(Sort.Direction.DESC, "cited_by_count"))          // 按引用量降序
                .withPageable(PageRequest.of(0, 100))                              // 取前100
                .build();

        SearchHits<Author> hits = elasticsearchOperations.search(query, Author.class);

        log.info("domainName, hits: ", domainName, hits.getTotalHits());
        if (hits.getTotalHits() == 0) return;

        // 2.2 准备数据
        List<Object[]> batchArgs = new ArrayList<>();
        for (SearchHit<Author> hit : hits) {
            Author author = hit.getContent();
            
            // 计算影响力分数
            double score = calculateInfluenceScore(author);
            int cited = author.getCitedByCount() != null ? author.getCitedByCount() : 0;
            // 拼接 primary_tags (将 List<String> 转为逗号分隔字符串)
            String tags = author.getField() != null ? 
                          String.join(", ", author.getField()) : "";
            if (tags.length() > 255) tags = tags.substring(0, 255); // 防止超长
            //System.out.println(tags);
            // 获取统计数据 (防止空指针)
            int hIndex = (author.getSummaryStats() != null && author.getSummaryStats().getHIndex() != null) 
                         ? author.getSummaryStats().getHIndex() : 0;
            int i10Index = (author.getSummaryStats() != null && author.getSummaryStats().getI10Index() != null) 
                           ? author.getSummaryStats().getI10Index() : 0;

            batchArgs.add(new Object[]{
                    author.getId(),
                    author.getDisplayName(),
                    tags,
                    hIndex,
                    i10Index,
                    author.getWorksCount(),
                    score,
                    cited
            });
        }

        // 2.3 动态建表并入库
        String tableName = sanitizeTableName(domainName);
        createTableIfNotExists(tableName);
        saveToDatabase(tableName, batchArgs);
    }

    /**
     * 简单的影响力计算公式
     * Score = (引用数 * 0.5) + (H指数 * 10) + (i10指数 * 5) + (作品数 * 1)
     */
    private double calculateInfluenceScore(Author author) {
        int cited = author.getCitedByCount() != null ? author.getCitedByCount() : 0;
        int works = author.getWorksCount() != null ? author.getWorksCount() : 0;
        int hIndex = 0;
        int i10Index = 0;

        if (author.getSummaryStats() != null) {
            hIndex = author.getSummaryStats().getHIndex() != null ? author.getSummaryStats().getHIndex() : 0;
            i10Index = author.getSummaryStats().getI10Index() != null ? author.getSummaryStats().getI10Index() : 0;
        }

        // 权重可根据实际业务调整
        return (cited * 0.5) + (hIndex * 10.0) + (i10Index * 5.0) + (works * 1.0);
    }

    /**
     * 将领域名称转换为合法的 MySQL 表名
     * 例如: "Computer Science" -> "scholar_ranking_computer_science"
     */
    private String sanitizeTableName(String domain) {
        // 转小写，替换非字母数字字符为下划线
        String safeName = domain.toLowerCase().replaceAll("[^a-z0-9]", "_");
        // 去除重复下划线
        safeName = safeName.replaceAll("_+", "_");
        // 去除首尾下划线
        if (safeName.startsWith("_")) safeName = safeName.substring(1);
        if (safeName.endsWith("_")) safeName = safeName.substring(0, safeName.length() - 1);
        
        return "scholar_ranking_" + safeName;
    }

    private void createTableIfNotExists(String tableName) {
        String sql = String.format("""
            CREATE TABLE IF NOT EXISTS `%s` (
              `id` VARCHAR(100) PRIMARY KEY,
              `display_name` VARCHAR(255),
              `primary_tags` VARCHAR(255),
              `h_index` INT,
              `i10_index` INT,
              `works_count` INT,
              `influence_score` DOUBLE,
              `cited_count` DOUBLE,
              INDEX `idx_score` (`influence_score` DESC)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """, tableName);
        
        jdbcTemplate.execute(sql);
    }

    private void saveToDatabase(String tableName, List<Object[]> batchArgs) {
        // 1. 清空旧数据 (因为是排行榜，通常是全量刷新)
        jdbcTemplate.execute("TRUNCATE TABLE `" + tableName + "`");

        // 2. 批量插入
        String sql = String.format("""
            INSERT INTO `%s` (id, display_name, primary_tags, h_index, i10_index, works_count, influence_score, cited_count)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """, tableName);

        jdbcTemplate.batchUpdate(sql, batchArgs);
        //log.info("表 {} 数据更新完成，插入 {} 条记录", tableName, batchArgs.size());
    }
}
