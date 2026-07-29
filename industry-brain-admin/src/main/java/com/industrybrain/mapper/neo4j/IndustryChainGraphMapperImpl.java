package com.industrybrain.mapper.neo4j;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class IndustryChainGraphMapperImpl implements IndustryChainGraphMapper {

    @Resource
    private Driver neo4jDriver;

    @Override
    public List<Map<String, Object>> findChainGraph(Long chainId) {
        List<Map<String, Object>> results = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            var result = session.run(
                "MATCH (ic:IndustryChain {chainId: $chainId})<-[:BELONGS_TO]-(cn:ChainNode) " +
                "OPTIONAL MATCH (cn)-[r:SUPPLIES_TO|DEMANDS_FROM]->(other:ChainNode) " +
                "RETURN cn, r, other",
                Map.of("chainId", chainId));
            result.stream().forEach(record -> results.add(record.asMap()));
        } catch (Exception e) {
            // 静默降级
        }
        return results;
    }

    @Override
    public List<Map<String, Object>> findNodesByDiagnosisTag(String tag, Long regionId) {
        List<Map<String, Object>> results = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            var result = session.run(
                "MATCH (cn:ChainNode {diagnosisTag: $tag}) " +
                "RETURN cn",
                Map.of("tag", tag));
            result.stream().forEach(record -> results.add(record.asMap()));
        } catch (Exception e) {
            // 静默降级
        }
        return results;
    }

    @Override
    public List<Map<String, Object>> findNodeRelations(Long nodeId) {
        List<Map<String, Object>> results = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            var result = session.run(
                "MATCH (cn:ChainNode {nodeId: $nodeId})-[r]->(other) " +
                "RETURN cn, type(r) AS relationType, other",
                Map.of("nodeId", nodeId));
            result.stream().forEach(record -> results.add(record.asMap()));
        } catch (Exception e) {
            // 静默降级
        }
        return results;
    }

    @Override
    public void upsertChainNode(Long nodeId) {
        // 由外部调用单条更新
    }
}
