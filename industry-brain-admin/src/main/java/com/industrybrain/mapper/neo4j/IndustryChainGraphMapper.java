package com.industrybrain.mapper.neo4j;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 产业链图谱查询（Neo4j）
 * 通过 Neo4jTemplate / Cypher 查询实现，见 Neo4jConfig
 *
 * @author Developer #4 (查英)
 */
@Repository
public interface IndustryChainGraphMapper {

    /** 查询某产业链的完整节点图谱 */
    List<Map<String, Object>> findChainGraph(Long chainId);

    /** 按诊断标签筛选节点 */
    List<Map<String, Object>> findNodesByDiagnosisTag(String tag, Long regionId);

    /** 查询节点间的供需关系 */
    List<Map<String, Object>> findNodeRelations(Long nodeId);

    /** 重建产业链节点+关系 */
    void upsertChainNode(Long nodeId);
}
