package com.industrybrain.mapper.elasticsearch;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 企业 ES 搜索（索引: industry_brain_enterprise）
 * 通过 ElasticsearchRestTemplate 实现，见 ElasticsearchConfig
 *
 * @author Developer #4 (查英)
 */
@Repository
public interface EnterpriseSearchMapper {

    /** 企业名称模糊搜索 */
    List<Map<String, Object>> searchByName(String keyword, int from, int size);

    /** 按区域+本地企业过滤 */
    List<Map<String, Object>> searchByRegion(Long regionId, Boolean isLocal, int from, int size);

    /** 重建企业索引 */
    void reindexAll();

    /** 按 ID 索引单条企业 */
    void indexById(Long enterpriseId);
}
