package com.industrybrain.mapper.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@ConditionalOnBean(name = "restClient")
public class EnterpriseSearchMapperImpl implements EnterpriseSearchMapper {

    private static final String INDEX = "industry_brain_enterprise";

    @Resource
    private ElasticsearchClient esClient;

    @Override
    public List<Map<String, Object>> searchByName(String keyword, int from, int size) {
        try {
            SearchResponse<Map> response = esClient.search(s -> s
                    .index(INDEX)
                    .query(q -> q.multiMatch(m -> m
                            .query(keyword)
                            .fields("name", "businessScope")))
                    .from(from)
                    .size(size),
                    Map.class);
            List<Map<String, Object>> results = new ArrayList<>();
            for (Hit<Map> hit : response.hits().hits()) {
                if (hit.source() != null) {
                    results.add(hit.source());
                }
            }
            return results;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override
    public List<Map<String, Object>> searchByRegion(Long regionId, Boolean isLocal, int from, int size) {
        try {
            SearchResponse<Map> response = esClient.search(s -> s
                    .index(INDEX)
                    .query(q -> q.bool(b -> {
                        b.must(m -> m.term(t -> t.field("regionId").value(regionId)));
                        if (isLocal != null) {
                            b.must(m -> m.term(t -> t.field("isLocal").value(isLocal)));
                        }
                        return b;
                    }))
                    .from(from)
                    .size(size),
                    Map.class);
            List<Map<String, Object>> results = new ArrayList<>();
            for (Hit<Map> hit : response.hits().hits()) {
                if (hit.source() != null) {
                    results.add(hit.source());
                }
            }
            return results;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override
    public void reindexAll() {
        // 由外部调用批量重建
    }

    @Override
    public void indexById(Long enterpriseId) {
        // 由外部调用单条索引
    }
}
