package com.industrybrain.mapper.neo4j;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 企业关系图谱查询（Neo4j）
 * 通过 Neo4jTemplate / Cypher 查询实现，见 Neo4jConfig
 *
 * @author Developer #4 (查英)
 */
@Repository
public interface EnterpriseRelationMapper {

    /** 查两企业间的最短路径 */
    List<Map<String, Object>> findShortestPath(Long fromEnterpriseId, Long toEnterpriseId);

    /** 查某企业的上下游关联企业 */
    List<Map<String, Object>> findUpstreamDownstream(Long enterpriseId, int depth);

    /** 按产业链节点查关联企业 */
    List<Map<String, Object>> findEnterprisesByNodeId(Long nodeId);

    /** 重建企业节点 */
    void upsertEnterpriseNode(Long enterpriseId);
}
