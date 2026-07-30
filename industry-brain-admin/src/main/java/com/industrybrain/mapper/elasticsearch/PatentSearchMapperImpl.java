package com.industrybrain.mapper.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@ConditionalOnBean(name = "restClient")
public class PatentSearchMapperImpl implements PatentSearchMapper {

    private static final String INDEX = "industry_brain_patent";

    @Resource
    private ElasticsearchClient esClient;

    @Override
    public List<Map<String, Object>> searchByKeyword(String keyword, int from, int size) {
        try {
            SearchResponse<Map> response = esClient.search(s -> s
                    .index(INDEX)
                    .query(q -> q.multiMatch(m -> m
                            .query(keyword)
                            .fields("patentName", "abstractText", "applicant")))
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
    public List<Map<String, Object>> searchByEnterprise(Long enterpriseId, int from, int size) {
        try {
            SearchResponse<Map> response = esClient.search(s -> s
                    .index(INDEX)
                    .query(q -> q.term(t -> t
                            .field("applicant_enterprise_id")
                            .value(enterpriseId)))
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
    public List<Map<String, Object>> aggregateByIpc() {
        try {
            SearchResponse<Map> response = esClient.search(s -> s
                    .index(INDEX)
                    .size(0)
                    .aggregations("by_ipc", a -> a
                            .terms(t -> t.field("ipcMain.keyword").size(100))),
                    Map.class);
            List<Map<String, Object>> result = new ArrayList<>();
            response.aggregations().forEach((name, agg) -> {
                agg.sterms().buckets().array().forEach(bucket -> {
                    result.add(Map.of("ipc", bucket.key().stringValue(), "count", bucket.docCount()));
                });
            });
            return result;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override
    public void reindexAll() {
        // 由外部调用批量重建，见 PatentService
    }

    @Override
    public void indexById(Long patentId) {
        // 由外部调用单条索引，见 PatentService
    }
}
