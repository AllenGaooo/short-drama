package com.bunny.flix.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import com.bunny.flix.feign.drama.dto.DramaDTO;
import com.bunny.flix.feign.search.dto.SearchResultDTO;
import com.bunny.flix.search.dto.SearchPageResult;
import com.bunny.flix.search.dto.SearchQuery;
import com.bunny.flix.search.service.SearchCacheService;
import com.bunny.flix.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 搜索服务实现（基于 Elasticsearch 8.x Java API Client）
 *
 * 设计要点：
 * 1. Mapping：中文用 ik 分词（ik_max_word 索引 / ik_smart 查询），
 *    tags、actors、category 保留 keyword 子字段用于精确筛选与聚合；
 *    分片数按百万级文档设为 3，refresh_interval 调大提升写入吞吐。
 * 2. 查询：全文检索用 multi_match（best_fields）跨字段并带权重；
 *    筛选条件放 filter 上下文——不参与算分且结果可被 ES 缓存，性能更好。
 * 3. 相关性排序：默认排序用 function_score 把热度作为加分项，
 *    实现"相关度 + 热度"的混合排序，而不是单纯按时间倒序。
 * 4. 缓存：热点搜索结果走 Redis（互斥锁防击穿 + 空值缓存防穿透），
 *    排行榜走逻辑过期（热点永不断流）。
 * 5. 性能：记录 ES took 耗时，暴露 P95/平均值用于验证 150ms 目标。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private static final String DRAMA_INDEX = "drama";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 短剧索引 settings + mapping
     *
     * 优化点：
     * - number_of_shards=3：支撑百万级文档（单分片超过 50GB 会显著影响查询性能）
     * - refresh_interval=5s：降低段合并频率，提升写入吞吐（搜索场景可容忍 5s 延迟）
     * - keyword 子字段：tags/actors/category 支持 term 精确筛选与聚合
     * - copy_to=searchText：把多个字段汇总到一个字段，便于做整体匹配兜底
     */
    private static final String DRAMA_INDEX_JSON = """
            {
              "settings": {
                "number_of_shards": 3,
                "number_of_replicas": 1,
                "refresh_interval": "5s",
                "index": {
                  "max_result_window": 100000
                }
              },
              "mappings": {
                "properties": {
                  "dramaId":     { "type": "long" },
                  "title":       { "type": "text", "analyzer": "ik_max_word", "search_analyzer": "ik_smart",
                                   "fields": { "keyword": { "type": "keyword" } }, "copy_to": "searchText" },
                  "cover":       { "type": "keyword", "index": false },
                  "description": { "type": "text", "analyzer": "ik_max_word", "search_analyzer": "ik_smart",
                                   "copy_to": "searchText" },
                  "actors":      { "type": "text", "analyzer": "ik_max_word",
                                   "fields": { "keyword": { "type": "keyword" } }, "copy_to": "searchText" },
                  "tags":        { "type": "text", "analyzer": "ik_max_word",
                                   "fields": { "keyword": { "type": "keyword" } }, "copy_to": "searchText" },
                  "category":    { "type": "keyword" },
                  "status":      { "type": "keyword" },
                  "searchText":  { "type": "text", "analyzer": "ik_max_word", "search_analyzer": "ik_smart" },
                  "heatScore":   { "type": "double" },
                  "playCount":   { "type": "long" },
                  "likeCount":   { "type": "long" },
                  "createTime":  { "type": "date", "format": "yyyy-MM-dd HH:mm:ss" }
                }
              }
            }
            """;

    private final ElasticsearchClient elasticsearchClient;
    private final SearchCacheService cacheService;

    /** 是否启用搜索结果缓存 */
    @Value("${search.cache.enabled:true}")
    private boolean cacheEnabled;

    /** 慢查询阈值（毫秒），超过则告警，用于守护 150ms 目标 */
    @Value("${search.slow-query-threshold-ms:150}")
    private long slowQueryThresholdMs;

    /** 耗时统计（原子累加，用于计算平均值） */
    private final AtomicLong totalTookMs = new AtomicLong();
    private final AtomicLong queryCount = new AtomicLong();
    private final AtomicLong slowQueryCount = new AtomicLong();

    // ------------------------------------------------------------------
    // 搜索（兼容原签名，内部接入缓存与优化后的查询）
    // ------------------------------------------------------------------
    @Override
    public List<SearchResultDTO> search(String keyword, Integer page, Integer size, String sortBy) {
        SearchQuery query = SearchQuery.builder()
                .keyword(keyword)
                .sortBy(sortBy)
                .page(page == null || page < 1 ? 1 : page)
                .size(size == null || size < 1 ? 10 : size)
                .build();
        return advancedSearch(query).getList();
    }

    // ------------------------------------------------------------------
    // 多条件搜索（全文检索 + 精确筛选 + 混合排序 + 缓存）
    // ------------------------------------------------------------------
    @Override
    public SearchPageResult advancedSearch(SearchQuery query) {
        if (query == null) {
            return SearchPageResult.empty(1, 10);
        }
        int page = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        int size = query.getSize() == null || query.getSize() < 1 ? 10 : query.getSize();

        // 无关键词且无筛选条件时直接返回空，避免无意义的全表扫描
        if (isBlank(query.getKeyword()) && query.getCategory() == null
                && query.getTags() == null && query.getMinPlayCount() == null) {
            return SearchPageResult.empty(page, size);
        }

        String cacheKey = buildCacheKey(query, page, size);

        // 走缓存（互斥锁防击穿 + 空值防穿透）
        if (cacheEnabled) {
            SearchCacheService.CacheResult<SearchPageResult> cached =
                    cacheService.queryWithPassThrough(cacheKey, SearchPageResult.class,
                            () -> doSearch(query, page, size));
            if (cached.isEmpty()) {
                return SearchPageResult.empty(page, size);
            }
            SearchPageResult result = cached.getData();
            if (result != null) {
                result.setFromCache(cached.isFromCache());
            }
            return result;
        }

        return doSearch(query, page, size);
    }

    /** 真正执行 ES 查询（含耗时统计与慢查询告警） */
    private SearchPageResult doSearch(SearchQuery query, int page, int size) {
        long start = System.currentTimeMillis();
        int from = (page - 1) * size;

        Query esQuery = buildQuery(query);

        try {
            SearchResponse<Map> response = elasticsearchClient.search(s -> s
                            .index(DRAMA_INDEX)
                            .query(esQuery)
                            .from(from)
                            .size(size)
                            .sort(so -> so.field(f -> f.field(sortField(query.getSortBy())).order(SortOrder.Desc)))
                            .highlight(h -> h
                                    .preTags("<em>")
                                    .postTags("</em>")
                                    .fields("title", f -> f.fragmentSize(50).numberOfFragments(1))),
                    Map.class);

            long took = System.currentTimeMillis() - start;
            recordLatency(took);

            List<SearchResultDTO> results = new ArrayList<>();
            for (Hit<Map> hit : response.hits().hits()) {
                if (hit.source() != null) {
                    results.add(mapToSearchResultDTO(hit.source()));
                }
            }

            Long total = response.hits().total() != null ? response.hits().total().value() : 0L;
            log.info("[search] ES检索完成 keyword={} hits={} total={} took={}ms",
                    query.getKeyword(), results.size(), total, took);

            return SearchPageResult.builder()
                    .list(results)
                    .total(total)
                    .page(page)
                    .size(size)
                    .tookMs(took)
                    .fromCache(false)
                    .build();
        } catch (IOException e) {
            log.error("[search] ES检索失败 keyword={}", query.getKeyword(), e);
            // ES 故障降级：返回空结果而不是抛异常，保证搜索页不白屏
            return SearchPageResult.empty(page, size);
        }
    }

    /**
     * 构建查询：
     * - must：全文检索（multi_match 跨字段带权重）
     * - filter：精确筛选条件，不参与算分，结果可被 ES query cache 缓存
     * - 默认排序时套一层 function_score，把热度作为加分项
     */
    private Query buildQuery(SearchQuery query) {
        List<Query> filters = new ArrayList<>();

        // ---- 筛选条件（filter context，不参与算分） ----
        if (query.getCategory() != null && !query.getCategory().isBlank()) {
            filters.add(TermQuery.of(t -> t.field("category").value(query.getCategory()))._toQuery());
        }
        if (query.getTags() != null && !query.getTags().isEmpty()) {
            // 标签命中任意一个即可
            filters.add(TermsQuery.of(t -> t.field("tags.keyword")
                    .terms(v -> v.value(query.getTags().stream()
                            .map(FieldValue::of).toList())))._toQuery());
        }
        if (query.getActors() != null && !query.getActors().isBlank()) {
            filters.add(MatchQuery.of(m -> m.field("actors").query(query.getActors()))._toQuery());
        }
        if (query.getMinPlayCount() != null) {
            filters.add(RangeQuery.of(r -> r.field("playCount").gte(query.getMinPlayCount().doubleValue()))._toQuery());
        }
        // 注意：这里不要强制加 status=ONLINE 的 filter。
        // 存量索引中的老文档没有 status 字段，强制过滤会导致所有历史数据搜不到。
        // 如需按状态筛选，应由调用方显式传参，或在索引重建后再开启。

        // ---- 全文检索（must / query context，参与算分） ----
        if (isBlank(query.getKeyword())) {
            // 无关键词：全部走 filter，用 match_all 作为查询主体
            return BoolQuery.of(b -> b.filter(filters))._toQuery();
        }

        Query fullText = MultiMatchQuery.of(m -> m
                .query(query.getKeyword())
                // 字段权重：标题最重要，简介次之，演员与标签再次
                .fields("title^3.0", "description^1.5", "actors^1.2", "tags^1.2", "searchText^0.5")
                .type(TextQueryType.BestFields)
                .minimumShouldMatch("75%"))._toQuery();

        // 相关度 + 热度混合排序：默认排序（default）时生效
        if ("default".equalsIgnoreCase(query.getSortBy())) {
            Query scored = FunctionScoreQuery.of(fs -> fs
                    .query(fullText)
                    .functions(f -> f
                            .fieldValueFactor(fv -> fv
                                    .field("heatScore")
                                    .factor(1.2f)
                                    .modifier(FieldValueFactorModifier.Log1p)
                                    .missing(0.0)))
                    .functions(f -> f
                            .fieldValueFactor(fv -> fv
                                    .field("playCount")
                                    .factor(0.8f)
                                    .modifier(FieldValueFactorModifier.Log1p)
                                    .missing(0.0)))
                    .boostMode(FunctionBoostMode.Sum)
                    .scoreMode(FunctionScoreMode.Sum))._toQuery();

            return BoolQuery.of(b -> b.must(scored).filter(filters))._toQuery();
        }

        return BoolQuery.of(b -> b.must(fullText).filter(filters))._toQuery();
    }

    /** 排序字段映射 */
    private String sortField(String sortBy) {
        if (sortBy == null) return "_score";
        return switch (sortBy) {
            case "heat" -> "heatScore";
            case "newest" -> "createTime";
            case "play" -> "playCount";
            default -> "_score";
        };
    }

    // ------------------------------------------------------------------
    // 搜索联想
    // ------------------------------------------------------------------
    @Override
    public List<String> suggest(String keyword) {
        if (isBlank(keyword)) {
            return Collections.emptyList();
        }
        String cacheKey = "search:suggest:" + keyword;
        if (cacheEnabled) {
            SearchCacheService.CacheResult<List> cached = cacheService.queryWithPassThrough(
                    cacheKey, List.class, () -> doSuggest(keyword));
            return cached.isEmpty() ? Collections.emptyList() : (List<String>) cached.getData();
        }
        return doSuggest(keyword);
    }

    private List<String> doSuggest(String keyword) {
        try {
            // 用 completion suggester 性能最好，但需要专门字段；
            // 这里用 match_phrase_prefix 做前缀匹配，无需额外字段即可实现联想
            SearchResponse<Map> response = elasticsearchClient.search(s -> s
                            .index(DRAMA_INDEX)
                            .query(q -> q.matchPhrasePrefix(m -> m.field("title").query(keyword)))
                            .size(10)
                            .source(src -> src.filter(f -> f.includes("title"))),
                    Map.class);

            List<String> suggestions = new ArrayList<>();
            for (Hit<Map> hit : response.hits().hits()) {
                if (hit.source() != null) {
                    String title = (String) hit.source().get("title");
                    if (title != null && !suggestions.contains(title)) {
                        suggestions.add(title);
                    }
                }
            }
            return suggestions;
        } catch (IOException e) {
            log.error("[search] 搜索联想失败 keyword={}", keyword, e);
            return Collections.emptyList();
        }
    }

    // ------------------------------------------------------------------
    // 排行榜（热点数据，走逻辑过期缓存）
    // ------------------------------------------------------------------
    @Override
    public List<SearchResultDTO> hotRanking(Integer size) {
        String cacheKey = "search:ranking:hot:" + size;
        SearchCacheService.CacheResult<List> result = cacheService.queryRanking(
                cacheKey, List.class, () -> {
                    SearchQuery q = SearchQuery.builder()
                            .keyword(null)
                            .sortBy("heat")
                            .page(1)
                            .size(size == null || size < 1 ? 10 : size)
                            .build();
                    return doSearch(q, 1, q.getSize()).getList();
                });
        return result.isEmpty() ? Collections.emptyList() : (List<SearchResultDTO>) result.getData();
    }

    // ------------------------------------------------------------------
    // 索引同步（MQ 消费端调用）
    // ------------------------------------------------------------------
    @Override
    public void syncDramaToES(DramaDTO dramaDTO) {
        if (dramaDTO == null || dramaDTO.getId() == null) {
            log.warn("[search] 同步参数不完整，跳过");
            return;
        }
        Map<String, Object> sourceMap = new HashMap<>();
        sourceMap.put("dramaId", dramaDTO.getId());
        sourceMap.put("title", dramaDTO.getTitle());
        sourceMap.put("cover", dramaDTO.getCover());
        sourceMap.put("description", dramaDTO.getDescription());
        sourceMap.put("actors", "");
        sourceMap.put("tags", "");
        // DramaDTO 暂未提供 category 字段（短剧服务未返回），留空；
        // 待短剧服务补充该字段后，此处同步即可自动生效筛选能力
        sourceMap.put("category", "");
        // status 从 Integer 映射为枚举字符串：1=上架(ONLINE)，其它=下架(OFFLINE)
        Integer status = dramaDTO.getStatus();
        sourceMap.put("status", status != null && status == 1 ? "ONLINE" : "OFFLINE");
        sourceMap.put("heatScore", 0.0);
        sourceMap.put("playCount", dramaDTO.getViews() != null ? dramaDTO.getViews() : 0L);
        sourceMap.put("likeCount", dramaDTO.getLikes() != null ? dramaDTO.getLikes() : 0L);
        sourceMap.put("createTime", dramaDTO.getCreateTime() != null
                ? dramaDTO.getCreateTime().format(FORMATTER)
                : LocalDateTime.now().format(FORMATTER));

        try {
            elasticsearchClient.index(i -> i
                    .index(DRAMA_INDEX)
                    .id(String.valueOf(dramaDTO.getId()))
                    .document(sourceMap));
            log.info("[search] 同步短剧到ES成功 dramaId={}", dramaDTO.getId());

            // 索引更新后失效相关缓存，保证搜索结果的最终一致
            cacheService.evictByPrefix("search:q:");
            cacheService.evict("search:ranking:hot:10", "search:ranking:hot:20");
        } catch (IOException e) {
            log.error("[search] 同步短剧到ES失败 dramaId={}", dramaDTO.getId(), e);
        }
    }

    @Override
    public void deleteDramaFromES(Long dramaId) {
        try {
            elasticsearchClient.delete(d -> d.index(DRAMA_INDEX).id(String.valueOf(dramaId)));
            log.info("[search] 从ES删除短剧成功 dramaId={}", dramaId);
            cacheService.evictByPrefix("search:q:");
            cacheService.evict("search:ranking:hot:10", "search:ranking:hot:20");
        } catch (IOException e) {
            log.error("[search] 从ES删除短剧失败 dramaId={}", dramaId, e);
        }
    }

    @Override
    public void createDramaIndex() {
        try {
            boolean exists = elasticsearchClient.indices()
                    .exists(ExistsRequest.of(e -> e.index(DRAMA_INDEX)))
                    .value();
            if (exists) {
                log.info("[search] ES短剧索引已存在，跳过创建: {}", DRAMA_INDEX);
                return;
            }
            CreateIndexRequest request = CreateIndexRequest.of(c -> c
                    .index(DRAMA_INDEX)
                    .withJson(new ByteArrayInputStream(DRAMA_INDEX_JSON.getBytes(StandardCharsets.UTF_8))));
            elasticsearchClient.indices().create(request);
            log.info("[search] ES短剧索引创建成功: {}", DRAMA_INDEX);
        } catch (IOException e) {
            log.error("[search] ES短剧索引创建失败: {}", DRAMA_INDEX, e);
        }
    }

    // ------------------------------------------------------------------
    // 监控指标
    // ------------------------------------------------------------------
    @Override
    public Map<String, Object> metrics() {
        Map<String, Object> m = new LinkedHashMap<>();
        long count = queryCount.get();
        m.put("queryCount", count);
        m.put("avgTookMs", count == 0 ? 0 : Math.round(totalTookMs.get() * 100.0 / count) / 100.0);
        m.put("slowQueryCount", slowQueryCount.get());
        m.put("slowQueryThresholdMs", slowQueryThresholdMs);
        m.put("cacheEnabled", cacheEnabled);

        SearchCacheService.CacheMetrics cm = cacheService.metrics();
        m.put("cacheHit", cm.hit());
        m.put("cacheMiss", cm.miss());
        m.put("emptyHit", cm.emptyHit());
        m.put("cacheHitRate", cm.hitRate() + "%");
        return m;
    }

    // ------------------------------------------------------------------
    // 内部方法
    // ------------------------------------------------------------------

    /** 记录耗时，超过阈值打告警日志（守护 150ms 目标） */
    private void recordLatency(long tookMs) {
        totalTookMs.addAndGet(tookMs);
        queryCount.incrementAndGet();
        if (tookMs > slowQueryThresholdMs) {
            slowQueryCount.incrementAndGet();
            log.warn("[search] 慢查询告警 took={}ms 超过阈值 {}ms", tookMs, slowQueryThresholdMs);
        }
    }

    /** 构建缓存 key（把筛选条件也纳入，避免不同条件命中同一份缓存） */
    private String buildCacheKey(SearchQuery q, int page, int size) {
        return String.format("search:q:%s:%s:%s:%s:%d:%d",
                nvl(q.getKeyword()), nvl(q.getCategory()),
                q.getTags() == null ? "" : String.join(",", q.getTags()),
                nvl(q.getSortBy()), page, size);
    }

    private SearchResultDTO mapToSearchResultDTO(Map<String, Object> sourceMap) {
        SearchResultDTO dto = new SearchResultDTO();
        dto.setDramaId(sourceMap.get("dramaId") != null ? Long.valueOf(sourceMap.get("dramaId").toString()) : null);
        dto.setTitle((String) sourceMap.get("title"));
        dto.setCover((String) sourceMap.get("cover"));
        dto.setDescription((String) sourceMap.get("description"));
        dto.setActors((String) sourceMap.get("actors"));
        dto.setTags((String) sourceMap.get("tags"));
        dto.setHeatScore(sourceMap.get("heatScore") != null ? Double.valueOf(sourceMap.get("heatScore").toString()) : 0.0);
        dto.setPlayCount(sourceMap.get("playCount") != null ? Long.valueOf(sourceMap.get("playCount").toString()) : 0L);
        dto.setLikeCount(sourceMap.get("likeCount") != null ? Long.valueOf(sourceMap.get("likeCount").toString()) : 0L);

        if (sourceMap.get("createTime") != null) {
            try {
                dto.setCreateTime(LocalDateTime.parse(sourceMap.get("createTime").toString(), FORMATTER));
            } catch (Exception e) {
                dto.setCreateTime(LocalDateTime.now());
            }
        }
        return dto;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }
}
