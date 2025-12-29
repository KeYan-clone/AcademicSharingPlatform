package com.scholar.platform.service;

import com.scholar.platform.dto.AchievementDTO;
import com.scholar.platform.entity.*;
import com.scholar.platform.repository.*;
import com.scholar.platform.service.cache.CachedPage;
import com.scholar.platform.service.cache.SearchCacheService;
import com.scholar.platform.util.CacheKeyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.ScriptType;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.scholar.platform.util.IdPrefixUtil;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.elasticsearch._types.query_dsl.*;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final InstitutionRepository institutionRepository;
    private final AuthorRepository authorRepository;
    // private final ConceptRepository conceptRepository;
    private final PaperKeywordRepository paperKeywordRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final UserRepository userRepository;
    private final UserCollectionRepository userCollectionRepository;
    private final TranslationService translationService;
    private final SearchCacheService searchCacheService;

    /**
     * 通过关键词搜索（带加权排序）
     * 使用 function_score 查询应用权重算法
     * 权重因子 = (log(1+cited_by_count)×1.2 + log(1+favourite_count)×1.0 +
     * log(1+read_count)×0.8) / 3
     */
    public Page<AchievementDTO> searchByKeyword(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("请输入检索内容");
        }

        if (translationService.containsChinese(keyword)) {
            String translatedKeyword = translationService.translateToEnglish(keyword);
            if (!keyword.equals(translatedKeyword)) {
                return achievementRepository.searchByTwoKeywords(keyword, translatedKeyword, pageable)
                        .map(this::toDTO);
            }
        }

        return achievementRepository.searchByKeywordWithSpaceSupport(keyword, pageable)
                .map(this::toDTO);
    }

    /**
     * 按照concepts精确匹配检索（支持带空格的完整短语，如 "Computer science"）
     * 使用 match_phrase 确保精确匹配，不会匹配到包含该词的其他概念
     */
    public Page<AchievementDTO> searchByConceptsExact(String concept, Pageable pageable) {
        if (concept == null || concept.trim().isEmpty()) {
            throw new IllegalArgumentException("请输入检索内容");
        }
        return achievementRepository.findByConceptsExactMatch(concept, pageable)
                .map(this::toDTO);
    }

    /**
     * 按时间范围检索（带加权排序）
     */
    public Page<AchievementDTO> searchByDateRange(String startDate, String endDate, Pageable pageable) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("起止时间不能为空");
        }
        // 由于时间范围检索没有关键词，使用非加权版本
        return achievementRepository.findByPublicationDateBetween(startDate, endDate, pageable)
                .map(this::toDTO);
    }

    /**
     * 高级检索：支持关键词、概念、时间范围、作者和机构的组合检索
     * 现在统一使用加权搜索作为默认功能
     *
     * @param keyword         关键词（模糊搜索标题和概念，带加权）
     * @param field           学科领域/概念（精确匹配，由用户从下拉列表选择）
     * @param startDate       开始日期
     * @param endDate         结束日期
     * @param authorName      作者姓名（精确匹配）
     * @param institutionName 机构名称（精确匹配）
     * @param pageable        分页参数
     */
    public Page<AchievementDTO> advancedSearch(String keyword, String field,
            String startDate, String endDate,
            String authorName, String institutionName,
            Pageable pageable) {
        String cacheKey = CacheKeyUtil.advancedSearchKey(keyword, field, startDate, endDate, authorName,
                institutionName, pageable);
        CachedPage<Achievement> cachedPage = searchCacheService.get(cacheKey);
        if (cachedPage != null) {
            return toDtoPageFromCache(cachedPage, pageable);
        }

        // 1. 预先解析 ID (暂缓搜索)
        String institutionId = null;
        if (institutionName != null && !institutionName.trim().isEmpty()) {
            Page<Institution> institutions = institutionRepository.findByDisplayName(institutionName,
                    Pageable.ofSize(1));
            if (institutions.hasContent()) {
                institutionId = institutions.getContent().get(0).getId();
            }
        }

        String authorId = null;
        if (authorName != null && !authorName.trim().isEmpty()) {
            Page<Author> authors = authorRepository.findByDisplayName(authorName, Pageable.ofSize(1));
            if (authors.hasContent()) {
                authorId = authors.getContent().get(0).getId();
            }
        }

        // 2. 构建核心 Bool 查询
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        // 3. 构建 Function Score 评分查询
        Query mainQuery = buildScoredQuery(keyword);
        boolBuilder.must(mainQuery);

        // 4. 添加各种过滤器 (Filters)
        List<Query> filters = new ArrayList<>();

        // 机构：name和id只要有一个匹配即可
        if ((institutionName != null && !institutionName.trim().isEmpty()) || institutionId != null) {
            List<Query> institutionShould = new ArrayList<>();
            if (institutionName != null && !institutionName.trim().isEmpty()) {
                institutionShould.addAll(buildNameQueries("institution_names", institutionName));
            }
            if (institutionId != null) {
                institutionShould.add(createTermQuery("institution_ids", institutionId));
            }
            if (!institutionShould.isEmpty()) {
                filters.add(QueryBuilders.bool(b -> b.should(institutionShould).minimumShouldMatch("1")));
            }
        }

        // 作者：name和id只要有一个匹配即可
        if ((authorName != null && !authorName.trim().isEmpty()) || authorId != null) {
            List<Query> authorShould = new ArrayList<>();
            if (authorName != null && !authorName.trim().isEmpty()) {
                authorShould.addAll(buildNameQueries("author_names", authorName));
            }
            if (authorId != null) {
                authorShould.add(createTermQuery("author_ids", authorId));
            }
            if (!authorShould.isEmpty()) {
                filters.add(QueryBuilders.bool(b -> b.should(authorShould).minimumShouldMatch("1")));
            }
        }

        // 修正这里：使用 concepts 而不是 concept
        if (field != null && !field.trim().isEmpty())
            filters.add(createMatchQuery("concepts", field)); // concepts 是数组字段

        if (startDate != null && endDate != null) {
            filters.add(QueryBuilders
                    .range(r -> r.field("publication_date").gte(JsonData.of(startDate)).lte(JsonData.of(endDate))));
        }

        // 5. 兜底逻辑：如果没有任何搜索条件，展示热门概念
        // 修正这里：检查的是 concepts 字段相关的条件
        if (isCriteriaEmpty(keyword, field, institutionName, institutionId, authorName, authorId)) {
            filters.add(getPopularConceptFilter()); // 这个方法需要返回对 concepts 数组的查询
        }

        boolBuilder.filter(filters);

        // 6. 执行 NativeQuery
        NativeQuery nativeQuery = new NativeQueryBuilder()
                .withQuery(boolBuilder.build()._toQuery())
                .withPageable(pageable)
                .build();

        // System.out.println("NativeQuery: " + nativeQuery.getQuery().toString());

        SearchHits<Achievement> hits = elasticsearchOperations.search(nativeQuery, Achievement.class);
        List<Achievement> achievements = hits.getSearchHits().stream()
            .map(hit -> hit.getContent())
            .collect(Collectors.toList());

        searchCacheService.put(cacheKey, CachedPage.of(achievements, hits.getTotalHits()));

        return toDtoPage(achievements, pageable, hits.getTotalHits());
    }

    private Page<AchievementDTO> toDtoPageFromCache(CachedPage<Achievement> cachedPage, Pageable pageable) {
        List<Achievement> records = cachedPage.getRecords();
        if (records == null) {
            records = Collections.emptyList();
        }
        return toDtoPage(records, pageable, cachedPage.getTotal());
    }

    private Page<AchievementDTO> toDtoPage(List<Achievement> achievements, Pageable pageable, long total) {
        List<AchievementDTO> list = achievements.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return new PageImpl<>(list, pageable, total);
    }

    /**
     * 构建带权重的评分查询
     */
    private Query buildScoredQuery(String keyword) {
        Query baseQuery;

        // 构建基础查询（使用BM25相关度）
        if (keyword != null && !keyword.trim().isEmpty()) {
            // 使用正确的方式构建BoolQuery
            baseQuery = QueryBuilders.bool(b -> {
                // title字段匹配（权重3）
                b.should(s -> s.match(m -> m
                        .field("title")
                        .query(keyword)
                        .boost(3.0f)));

                // abstractText字段匹配（权重2）
                b.should(s -> s.match(m -> m
                        .field("abstractText")
                        .query(keyword)
                        .boost(2.0f)));

                // 处理中英翻译加权
                if (translationService != null && translationService.containsChinese(keyword)) {
                    String translated = translationService.translateToEnglish(keyword);

                    // 翻译后的title查询
                    b.should(s -> s.match(m -> m
                            .field("title")
                            .query(translated)
                            .boost(3.0f)));

                    // 翻译后的abstract查询
                    b.should(s -> s.match(m -> m
                            .field("abstractText")
                            .query(translated)
                            .boost(2.0f)));
                }

                b.minimumShouldMatch("1");
                return b;
            });

        } else {
            baseQuery = QueryBuilders.matchAll(m -> m);
        }

        // 简化脚本 - 假设三个count字段都确保存在
        String scriptCode = "double c = " +
                "doc.containsKey('citedByCount') ? " +
                "Math.log1p(doc['citedByCount'].value) * 1.2 : 0.0; " +
                "double f = doc.containsKey('favouriteCount') ?" +
                " Math.log1p(doc['favouriteCount'].value) * 1.0 : 0.0; " +
                "double r = doc.containsKey('readCount') ?" +
                " Math.log1p(doc['readCount'].value) * 0.8 : 0.0; " +
                "return 1.0 + (c + f + r) / 3.0;";

        // 创建函数评分查询
        return QueryBuilders.functionScore(fs -> fs
                .query(baseQuery)
                .functions(
                        f -> f.scriptScore(ss -> ss
                                .script(s -> s
                                        .inline(i -> i
                                                .source(scriptCode)
                                                .lang("painless")))))
                .scoreMode(FunctionScoreMode.Multiply)
                .boostMode(FunctionBoostMode.Multiply));
    }

    private Query createTermQuery(String field, String value) {
        return QueryBuilders.term(t -> t.field(field).value(FieldValue.of(value)));
    }

    private Query createMatchQuery(String field, String value) {
        return QueryBuilders.match(m -> m.field(field).query(value));
    }

    private List<Query> buildNameQueries(String fieldBase, String value) {
        String trimmed = value == null ? null : value.trim();
        if (trimmed == null || trimmed.isEmpty()) {
            return Collections.emptyList();
        }

        List<Query> queries = new ArrayList<>();
        // 优先尝试 keyword 子字段匹配，若不存在则不会命中，但不会报错
        queries.add(QueryBuilders.term(t -> t.field(fieldBase + ".keyword").value(FieldValue.of(trimmed))));
        // 同时保留直接 term 匹配
        queries.add(QueryBuilders.term(t -> t.field(fieldBase).value(FieldValue.of(trimmed))));
        // 再补一个 match_phrase 支持分词字段
        queries.add(QueryBuilders.matchPhrase(m -> m.field(fieldBase).query(trimmed)));
        return queries;
    }

    private boolean isCriteriaEmpty(String... params) {
        return Arrays.stream(params).allMatch(p -> p == null || p.trim().isEmpty());
    }

    private Query getPopularConceptFilter() {
        PaperKeyword pk = paperKeywordRepository.findFirstByOrderByCntDesc();

        // 简单的 null 检查
        Objects.requireNonNull(pk, "搜索内容不能为空且系统暂无推荐数据");

        // 直接返回 term 查询
        return QueryBuilders.match(m -> m
            .field("concepts")
            .query(pk.getKeyword()));
    }

    // /**
    //  * 辅助方法：在内存中过滤结果
    //  */
    // private Page<AchievementDTO> filterResults(Page<Achievement> results, String field,
    //         String authorId, String institutionId,
    //         Pageable pageable) {
    //     return filterResults(results, field, authorId, institutionId, pageable, null, null);
    // }

    // /**
    //  * 辅助方法：在内存中过滤结果（包含日期范围）
    //  */
    // private Page<AchievementDTO> filterResults(Page<Achievement> results, String field,
    //         String authorId, String institutionId,
    //         Pageable pageable, String startDate, String endDate) {
    //     List<AchievementDTO> filtered = results.getContent().stream()
    //             .filter(achievement -> {
    //                 // 按概念过滤
    //                 if (field != null && !field.trim().isEmpty()) {
    //                     if (achievement.getConcepts() == null ||
    //                             !achievement.getConcepts().contains(field)) {
    //                         return false;
    //                     }
    //                 }

    //                 // 按作者过滤
    //                 if (authorId != null) {
    //                     if (achievement.getAuthorIds() == null ||
    //                             !achievement.getAuthorIds().contains(authorId)) {
    //                         return false;
    //                     }
    //                 }

    //                 // 按机构过滤
    //                 if (institutionId != null) {
    //                     if (achievement.getInstitutionIds() == null ||
    //                             !achievement.getInstitutionIds().contains(institutionId)) {
    //                         return false;
    //                     }
    //                 }

    //                 // 按日期范围过滤
    //                 if (startDate != null && endDate != null && achievement.getPublicationDate() != null) {
    //                     String pubDate = achievement.getPublicationDate();
    //                     if (pubDate.compareTo(startDate) < 0 || pubDate.compareTo(endDate) > 0) {
    //                         return false;
    //                     }
    //                 }

    //                 return true;
    //             })
    //             .map(this::toDTO)
    //             .collect(Collectors.toList());

    //     return new PageImpl<>(filtered, pageable, results.getTotalElements());
    // }

    /**
     * 获取成果详情
     * 同时更新阅读次数和概念热度统计
     */
    public AchievementDTO getById(String id) {
        Achievement achievement = achievementRepository.findById(IdPrefixUtil.ensureIdPrefix(id))
                .orElseThrow(() -> new RuntimeException("成果不存在"));

        incrementReadCount(achievement);
        // updateConcept(achievement);

        return toDTO(achievement);
    }

    /**
     * 增加成果的阅读次数
     * 使用 Elasticsearch 脚本进行原子更新，避免并发覆盖问题
     */
    private void incrementReadCount(Achievement achievement) {
        updateEsFieldCount(achievement.getId(), "readCount", 1);

        if (achievement.getReadCount() == null) {
            achievement.setReadCount(1);
        } else {
            achievement.setReadCount(achievement.getReadCount() + 1);
        }
    }

    public List<AchievementDTO> getByIds(List<String> ids) {
        List<String> prefixedIds = ids.stream().map(IdPrefixUtil::ensureIdPrefix).collect(Collectors.toList());
        Iterable<Achievement> achievements = achievementRepository.findAllById(prefixedIds);
        List<AchievementDTO> dtos = new ArrayList<>();
        achievements.forEach(a -> dtos.add(toDTO(a)));
        return dtos;
    }

    public void incrementFavouriteCount(String achievementId) {
        updateEsFieldCount(IdPrefixUtil.ensureIdPrefix(achievementId), "favouriteCount", 1);
    }

    public void decrementFavouriteCount(String achievementId) {
        updateEsFieldCount(IdPrefixUtil.ensureIdPrefix(achievementId), "favouriteCount", -1);
    }

    private void updateEsFieldCount(String id, String field, int delta) {
        try {
            String scriptCode;
            if (delta > 0) {
                scriptCode = String.format(
                        "if (ctx._source.%s == null) { ctx._source.%s = %d } else { ctx._source.%s += %d }", field,
                        field, delta, field, delta);
            } else {
                scriptCode = String.format("if (ctx._source.%s != null && ctx._source.%s > 0) { ctx._source.%s += %d }",
                        field, field, field, delta);
            }

            UpdateQuery updateQuery = UpdateQuery.builder(id)
                    .withScript(scriptCode)
                    .withLang("painless")
                    .withScriptType(ScriptType.INLINE)
                    .build();

            elasticsearchOperations.update(updateQuery, IndexCoordinates.of("openalex_works"));
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to update " + field + " for achievement: " + id + ", error: " + e.getMessage());
        }
    }

    /**
     * 转换为DTO，提取作者信息
     */
    public AchievementDTO toDTO(Achievement achievement) {
        AchievementDTO dto = Achievement.toDTO(achievement);

        // Check if current user has favourited this achievement
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getPrincipal())) {
                String email = authentication.getName();
                userRepository.findByEmail(email).ifPresent(user -> {
                    boolean isFav = userCollectionRepository.existsByUserIdAndAchievementId(user.getId(),
                            IdPrefixUtil.removeIdPrefix(achievement.getId()));
                    dto.setIsFavourite(isFav);
                });
            } else {
                dto.setIsFavourite(false);
            }
        } catch (Exception e) {
            dto.setIsFavourite(false);
        }
        return dto;

    }

    public List<AchievementDTO> getPendingAchievements() {
        return achievementRepository.findByStatus(Achievement.AchievementStatus.PENDING)
                .stream()
                .map(Achievement::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public Achievement approveAchievement(String achievementId, String adminId) {
        Achievement achievement = achievementRepository.findById(achievementId)
                .orElseThrow(() -> new RuntimeException("成果不存在"));

        userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("管理员不存在"));

        achievement.setStatus(Achievement.AchievementStatus.APPROVED);
        return achievementRepository.save(achievement);
    }

    @Transactional
    public Achievement rejectAchievement(String achievementId, String adminId, String reason) {
        Achievement achievement = achievementRepository.findById(achievementId)
                .orElseThrow(() -> new RuntimeException("成果不存在"));

        userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("管理员不存在"));

        achievement.setStatus(Achievement.AchievementStatus.REJECTED);
        return achievementRepository.save(achievement);
    }

}
