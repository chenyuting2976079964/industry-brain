package com.industrybrain.mapper.neo4j;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class EnterpriseRelationMapperImpl implements EnterpriseRelationMapper {

    @Resource
    private Driver neo4jDriver;

    @Override
    public List<Map<String, Object>> findShortestPath(Long fromEnterpriseId, Long toEnterpriseId) {
        List<Map<String, Object>> results = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            var result = session.run(
                "MATCH (a:Enterprise {enterpriseId: $from}), (b:Enterprise {enterpriseId: $to}), " +
                "p = shortestPath((a)-[*..6]-(b)) " +
                "RETURN nodes(p) AS pathNodes, relationships(p) AS pathRels",
                Map.of("from", fromEnterpriseId, "to", toEnterpriseId));
            result.stream().forEach(record -> results.add(record.asMap()));
        } catch (Exception e) {
            // 静默降级
        }
        return results;
    }

    @Override
    public List<Map<String, Object>> findUpstreamDownstream(Long enterpriseId, int depth) {
        List<Map<String, Object>> results = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            var result = session.run(
                "MATCH (e:Enterprise {enterpriseId: $eid})-[*1.." + depth + "]-(related) " +
                "RETURN DISTINCT related",
                Map.of("eid", enterpriseId));
            result.stream().forEach(record -> results.add(record.asMap()));
        } catch (Exception e) {
            // 静默降级
        }
        return results;
    }

    @Override
    public List<Map<String, Object>> findEnterprisesByNodeId(Long nodeId) {
        List<Map<String, Object>> results = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            var result = session.run(
                "MATCH (e:Enterprise)-[:PARTICIPATES_IN]->(cn:ChainNode {nodeId: $nodeId}) " +
                "RETURN e",
                Map.of("nodeId", nodeId));
            result.stream().forEach(record -> results.add(record.asMap()));
        } catch (Exception e) {
            // 静默降级
        }
        return results;
    }

    @Override
    public void upsertEnterpriseNode(Long enterpriseId) {
        // 由外部调用单条更新
    }
}
