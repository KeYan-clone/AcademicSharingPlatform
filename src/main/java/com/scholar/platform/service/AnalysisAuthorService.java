package com.scholar.platform.service;

import com.scholar.platform.dto.AuthorInfluenceDTO;
import com.scholar.platform.dto.ScholarRankingDTO;
import com.scholar.platform.entity.Author;
import com.scholar.platform.entity.ScholarInfluence;
import com.scholar.platform.entity.ScholarRanking;
import com.scholar.platform.entity.User;
import com.scholar.platform.repository.AuthorRepository;
import com.scholar.platform.repository.ScholarInfluenceRepository;
import com.scholar.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scholar.platform.repository.ScholarRankingRepository;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;
import com.scholar.platform.dto.AuthorRelationDTO;
import com.scholar.platform.entity.AuthorRelation;
import com.scholar.platform.repository.AuthorRelationRepository;

import java.util.Collections;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisAuthorService {

    private final ScholarInfluenceRepository scholarInfluenceRepository;
    private final UserRepository userRepository;
    private final AuthorRepository authorRepository; // ES Repository
    private final ScholarRankingRepository scholarRankingRepository;
    private final AuthorRelationRepository authorRelationRepository;
    
    private final JdbcTemplate jdbcTemplate;
    @Transactional
    public AuthorInfluenceDTO getAuthorTrend(String userId) {
        // 1. 尝试从 MySQL 缓存表中获取
        return scholarInfluenceRepository.findById(userId)
                .map(this::toDTO)
                .orElseGet(() -> fetchFromEsAndSave(userId));
    }

    private AuthorInfluenceDTO fetchFromEsAndSave(String userId) {
        // 2. 获取用户信息
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + userId));
        
        String searchName = user.getUsername(); // 或者用 user.getRealName() 取决于你存的是什么
        log.info("MySQL缓存未命中，正在ES中搜索作者: {}", searchName);


        Sort sort = Sort.by(Sort.Direction.DESC, "cited_by_count");
        
        // PageRequest.of(页码, 每页数量, 排序规则)
        // 取第0页，第1条，按引用量降序 -> 这样就能取到引用量最高的那个 "Li Fei-Fei"
        Page<Author> authorPage = authorRepository.findByDisplayNameFuzzy(searchName, PageRequest.of(0, 1, sort));
        
        //List<Author> authors = authorRepository.findByDisplayName(searchName, PageRequest.of(0, 1)).getContent();
        List<Author> authors = authorPage.getContent();

        if (authors.isEmpty()) {
            // ES 中也没找到，返回空对象或抛异常，这里返回全0数据
            log.warn("ES中未找到作者: {}", searchName);
            return createEmptyDTO(user.getUsername());
        }

        Author esAuthor = authors.get(0);

        // 4. 整理数据并存入 MySQL
        ScholarInfluence influence = new ScholarInfluence();
        influence.setUserId(userId);
        influence.setOpenAlexId(esAuthor.getId());
        influence.setAuthorName(esAuthor.getDisplayName());
        influence.setWorksCount(esAuthor.getWorksCount() != null ? esAuthor.getWorksCount() : 0);
        influence.setCitedByCnt(esAuthor.getCitedByCount() != null ? esAuthor.getCitedByCount() : 0);
        
        if (esAuthor.getSummaryStats() != null) {
            influence.setHIndex(esAuthor.getSummaryStats().getHIndex());
            influence.setI10Index(esAuthor.getSummaryStats().getI10Index());
        } else {
            influence.setHIndex(0);
            influence.setI10Index(0);
        }

        if (esAuthor.getField() != null && !esAuthor.getField().isEmpty()) {
            // 1. Domain (一级学科): 取 fields 列表的第一个元素
            // 例如: ["Materials Science", "Psychology"] -> "Materials Science"
            influence.setDomain(esAuthor.getField().get(0));

            // 2. Topics (研究方向): 将整个 fields 列表拼接成字符串
            // 例如: "Materials Science, Psychology"
            //String topicStr = esAuthor.getFields().stream()
              //      .limit(5) // 限制长度，防止过长
               //     .collect(Collectors.joining(", "));
            //influence.setTopics(topicStr);
        } else {
            influence.setDomain("Unknown");
            //influence.setTopics("");
        }

        scholarInfluenceRepository.save(influence);
        log.info("已将ES数据同步至MySQL缓存, userId: {}", userId);

        return toDTO(influence);
    }

    private AuthorInfluenceDTO toDTO(ScholarInfluence entity) {
        AuthorInfluenceDTO dto = new AuthorInfluenceDTO();
        dto.setAuthorName(entity.getAuthorName());
        dto.setWorksCount(entity.getWorksCount());
        dto.setCitedByCnt(entity.getCitedByCnt());
        dto.setHIndex(entity.getHIndex());
        dto.setI10Index(entity.getI10Index());

        dto.setDomain(entity.getDomain());
        
        

        return dto;
    }

    private AuthorInfluenceDTO createEmptyDTO(String name) {
        AuthorInfluenceDTO dto = new AuthorInfluenceDTO();
        dto.setAuthorName(name);
        dto.setWorksCount(0);
        dto.setCitedByCnt(0);
        dto.setHIndex(0);
        dto.setI10Index(0);
        return dto;
    }

    public List<ScholarRankingDTO> getScholarRanking(String domain) {
        String tableName;
        
        if ("all".equalsIgnoreCase(domain)) {
            tableName = "scholar_ranking_all";
        } else {
            tableName = sanitizeTableName(domain);
        }

        
        String sql = "SELECT * FROM `" + tableName + "` ORDER BY influence_score DESC LIMIT 100";

        try {
            // 3. 执行查询并手动映射结果
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                ScholarRankingDTO dto = new ScholarRankingDTO();
                // 1. 设置外层属性
                dto.setInfluenceScore(rs.getDouble("influence_score"));
                dto.setRank(rowNum + 1); // 设置排名

                // 2. 创建并设置内层 ScholarInfo 对象
                ScholarRankingDTO.ScholarInfo info = new ScholarRankingDTO.ScholarInfo();
                info.setId(rs.getString("id"));
                info.setDisplayName(rs.getString("display_name"));
                info.setHIndex(rs.getInt("h_index"));
                info.setI10Index(rs.getInt("i10_index"));
                info.setWorksCount(rs.getInt("works_count"));
                info.setCitedCount(rs.getInt("cited_count"));

                // 3. 处理 tags: 数据库存的是字符串 "Tag1, Tag2"，转为 List
                String tagsStr = rs.getString("primary_tags");
                if (tagsStr != null && !tagsStr.isEmpty()) {
                    info.setPrimaryTags(Arrays.asList(tagsStr.split(",\\s*")));
                } else {
                    info.setPrimaryTags(Collections.emptyList());
                }

                dto.setScholar(info);
                return dto;
            });
        } catch (Exception e) {
            // 如果表不存在（比如用户输入了错误的领域），返回空列表而不是报错
            log.error("查询排行榜失败，表名: {}, 错误: {}", tableName, e.getMessage());
            return Collections.emptyList();
        }
    }

    private String sanitizeTableName(String domain) {
        if (domain == null) return "scholar_ranking_all";
        String safeName = domain.toLowerCase().replaceAll("[^a-z0-9]", "_");
        safeName = safeName.replaceAll("_+", "_");
        if (safeName.startsWith("_")) safeName = safeName.substring(1);
        if (safeName.endsWith("_")) safeName = safeName.substring(0, safeName.length() - 1);
        return "scholar_ranking_" + safeName;
    }

    /**
     * 根据作者名模糊查询作者关系（author1_name 或 author2_name 匹配）
     */
    public List<AuthorRelationDTO> getAuthorRelation(String authorName) {
        List<AuthorRelation> list1 = authorRelationRepository.findByAuthor1Name(authorName);
        List<AuthorRelation> list2 = authorRelationRepository.findByAuthor2Name(authorName);
        List<AuthorRelation> merged = new ArrayList<>(list1);
        for (AuthorRelation ar : list2) {
            if (!merged.contains(ar)) {
                merged.add(ar);
            }
        }

        return merged.stream().map(ar -> {
            AuthorRelationDTO dto = new AuthorRelationDTO();
            dto.setAuthor1Id(ar.getAuthor1Id());
            dto.setAuthor1Name(ar.getAuthor1Name());
            dto.setAuthor2Id(ar.getAuthor2Id());
            dto.setAuthor2Name(ar.getAuthor2Name());
            dto.setCount(ar.getCount());
            return dto;
        }).collect(Collectors.toList());
    }
}

