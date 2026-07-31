package com.industrybrain.service.impl;

import com.industrybrain.domain.model.*;
import com.industrybrain.mapper.mysql.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Neo4j 图谱同步服务
 * 将 MySQL 中由 GraphBuildService 生成的产业链图谱数据同步到 Neo4j：
 * - IndustryChain 产业链节点
 * - ChainNode 环节节点 + BELONGS_TO 关系
 * - ChainNodeLink 上下游连线 → SUPPLIES_TO 关系
 * - Enterprise 企业节点 + PARTICIPATES_IN 关系（按节点抽样，避免全量爆炸）
 *
 * @author 产业大脑平台
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(Driver.class)
public class Neo4jSyncService {

    private final Driver neo4jDriver;
    private final ChainInfoMapper chainInfoMapper;
    private final ChainNodeMapper chainNodeMapper;
    private final ChainNodeLinkMapper chainNodeLinkMapper;
    private final EnterpriseNodeMapper enterpriseNodeMapper;
    private final NodeDiagnosisMapper nodeDiagnosisMapper;
    private final EnterpriseMapper enterpriseMapper;

    /**
     * 全量同步 MySQL 图谱 → Neo4j
     */
    public void syncAll() {
        try (Session session = neo4jDriver.session()) {
            // 1. 清空旧数据
            session.run("MATCH (n) DETACH DELETE n");
            log.info("Neo4j 旧数据已清空");

            // 2. 产业链节点
            List<IndustryChain> chains = chainInfoMapper.selectAllOrdered();
            for (IndustryChain c : chains) {
                session.run(
                    "MERGE (ic:IndustryChain {chainId: $id}) SET ic.name = $name, ic.code = $code",
                    Map.of("id", c.getId(), "name", c.getName(), "code", c.getCode() == null ? "" : c.getCode()));
            }
            log.info("Neo4j 产业链节点: {}", chains.size());

            // 3. 环节节点 + BELONGS_TO + 诊断属性
            List<ChainNode> nodes = chainNodeMapper.selectList(null);
            for (ChainNode n : nodes) {
                session.run(
                    "MERGE (cn:ChainNode {nodeId: $id}) " +
                    "SET cn.name = $name, cn.layer = $layer, cn.sortOrder = $sort " +
                    "WITH cn " +
                    "MATCH (ic:IndustryChain {chainId: $chainId}) " +
                    "MERGE (cn)-[:BELONGS_TO]->(ic)",
                    Map.of("id", n.getId(), "name", n.getNodeName(), "layer",
                            n.getLayer() == null ? "" : n.getLayer(),
                            "sort", n.getSortOrder() == null ? 0 : n.getSortOrder(),
                            "chainId", n.getChainId()));
            }

            // 4. 诊断标签属性
            List<NodeDiagnosis> diags = nodeDiagnosisMapper.selectAllLatest();
            for (NodeDiagnosis d : diags) {
                session.run(
                    "MATCH (cn:ChainNode {nodeId: $id}) " +
                    "SET cn.diagnosisTag = $tag, cn.enterpriseCount = $ent, cn.patentCount = $pat",
                    Map.of("id", d.getNodeId(), "tag", d.getDiagnosisTag(),
                            "ent", d.getEnterpriseCount() == null ? 0 : d.getEnterpriseCount(),
                            "pat", d.getTotalPatentCount() == null ? 0 : d.getTotalPatentCount()));
            }
            log.info("Neo4j 环节节点: {}", nodes.size());

            // 5. 上下游连线 → SUPPLIES_TO
            List<ChainNodeLink> links = chainNodeLinkMapper.selectAll();
            for (ChainNodeLink l : links) {
                session.run(
                    "MATCH (a:ChainNode {nodeId: $src}), (b:ChainNode {nodeId: $tgt}) " +
                    "MERGE (a)-[:SUPPLIES_TO]->(b)",
                    Map.of("src", l.getSourceNodeId(), "tgt", l.getTargetNodeId()));
            }
            log.info("Neo4j 上下游连线: {}", links.size());

            // 6. 企业节点 + PARTICIPATES_IN（每节点取前 N 家，避免全量爆炸）
            List<ChainNode> sampleNodes = nodes;
            int entLinked = 0;
            for (ChainNode node : sampleNodes) {
                List<Map<String, Object>> topEnts = chainNodeMapper.selectTopEnterprisesByNodeId(node.getId(), 20);
                if (topEnts == null || topEnts.isEmpty()) continue;
                for (Map<String, Object> row : topEnts) {
                    Object eid = row.get("enterprise_id");
                    if (eid == null) continue;
                    Object ename = row.get("enterprise_name");
                    session.run(
                        "MERGE (e:Enterprise {enterpriseId: $eid}) SET e.name = $name " +
                        "WITH e " +
                        "MATCH (cn:ChainNode {nodeId: $nid}) " +
                        "MERGE (e)-[:PARTICIPATES_IN]->(cn)",
                        Map.of("eid", Long.valueOf(eid.toString()), "name",
                                ename == null ? "" : ename.toString(), "nid", node.getId()));
                    entLinked++;
                }
            }
            log.info("Neo4j 企业-环节关联: {}", entLinked);

        } catch (Exception e) {
            log.error("Neo4j 图谱同步失败: {}", e.getMessage(), e);
        }
    }
}
