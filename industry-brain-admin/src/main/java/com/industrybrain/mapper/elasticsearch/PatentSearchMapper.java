package com.industrybrain.mapper.elasticsearch;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 专利 ES 搜索（索引: industry_brain_patent）
 * 通过 ElasticsearchRestTemplate 实现，见 ElasticsearchConfig
 *
 * @author Developer #4 (查英)
 */
@Repository
public interface PatentSearchMapper {

    /** 专利全文字段搜索 */
    List<Map<String, Object>> searchByKeyword(String keyword, int from, int size);

    /** 按企业ID查专利 */
    List<Map<String, Object>> searchByEnterprise(Long enterpriseId, int from, int size);

    /** 按IPC分类统计 */
    List<Map<String, Object>> aggregateByIpc();

    /** 重建专利索引 */
    void reindexAll();

    /** 按 ID 索引单条专利 */
    void indexById(Long patentId);
}
