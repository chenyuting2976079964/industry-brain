package com.industrybrain.service.impl;

import com.industrybrain.domain.model.*;
import com.industrybrain.dto.response.*;
import com.industrybrain.service.IIndustryChainService;
import com.industrybrain.common.utils.ChainUtils;
import com.industrybrain.mapper.mysql.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 产业链全景业务实现 (v2.0 合并版 — C01~C09)
 * @author 何锦鹏
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndustryChainServiceImpl implements IIndustryChainService {

    private final ChainInfoMapper chainInfoMapper;
    private final ChainNodeMapper chainNodeMapper;
    private final ChainNodeLinkMapper chainNodeLinkMapper;
    private final NodeDiagnosisMapper nodeDiagnosisMapper;
    private final EnterpriseMapper enterpriseMapper;

    // ─── C01: 产业链目录列表 ───
    @Override
    @Cacheable(value = "chain:list", unless = "#result == null || #result.isEmpty()")
    public List<ChainResponse> getChainList() {
        List<IndustryChain> chains = chainInfoMapper.selectAllOrdered();
        if (chains == null || chains.isEmpty()) return Collections.emptyList();
        return chains.stream().map(c -> {
            ChainResponse vo = new ChainResponse();
            vo.setId(c.getId());
            vo.setChainName(c.getName());
            vo.setChainCode(c.getCode());

            List<NodeDiagnosis> latestResults = nodeDiagnosisMapper.selectLatestByChainId(c.getId());
            if (latestResults == null) latestResults = Collections.emptyList();
            int strong = 0, supplement = 0, weak = 0, missing = 0;
            int totalPatents = 0, totalEnterprises = 0;
            for (NodeDiagnosis r : latestResults) {
                totalPatents += r.getTotalPatentCount() != null ? r.getTotalPatentCount() : 0;
                totalEnterprises += r.getEnterpriseCount() != null ? r.getEnterpriseCount() : 0;
                String tag = r.getDiagnosisTag();
                if ("强链".equals(tag)) strong++;
                else if ("补链".equals(tag)) supplement++;
                else if ("弱链".equals(tag)) weak++;
                else if ("缺链".equals(tag)) missing++;
            }
            vo.setNodeCount(latestResults.size());
            vo.setTotalPatents(totalPatents);
            vo.setTotalEnterprises(totalEnterprises);
            vo.setStrongCount(strong);
            vo.setSupplementCount(supplement);
            vo.setWeakCount(weak);
            vo.setMissingCount(missing);
            return vo;
        }).collect(Collectors.toList());
    }

    /** C02: 产业链全部节点 (含诊断标签) */
    @Override
    public List<ChainNodeResponse> getChainNodes(Long chainId) {
        List<ChainNode> nodes = chainNodeMapper.selectByChainId(chainId);
        if (nodes == null) return Collections.emptyList();
        List<NodeDiagnosis> latestResults = nodeDiagnosisMapper.selectLatestByChainId(chainId);
        if (latestResults == null) latestResults = Collections.emptyList();
        Map<Long, NodeDiagnosis> resultMap = latestResults.stream()
            .collect(Collectors.toMap(NodeDiagnosis::getNodeId, r -> r, (a, b) -> a));

        return nodes.stream().map(n -> {
            ChainNodeResponse vo = new ChainNodeResponse();
            vo.setId(n.getId());
            vo.setChainId(n.getChainId());
            vo.setNodeName(n.getNodeName());
            // 前端 levelMap = {1:'up', 2:'mid', 3:'down'}，nodeLevel 发送 "1"/"2"/"3"
            vo.setNodeLevel(layerToCode(n.getLayer()));
            vo.setNodeLevelCode(n.getLayer() != null ? n.getLayer() : "0");
            vo.setNodeLevelLabel(ChainUtils.layerToLabel(n.getLayer()));
            vo.setSortOrder(n.getSortOrder());

            NodeDiagnosis dr = resultMap.get(n.getId());
            if (dr != null) {
                vo.setDiagnosisTag(dr.getDiagnosisTag());
                vo.setDiagnosisLabel(dr.getDiagnosisTag());
                vo.setDiagnosisColor(ChainUtils.tagToColor(dr.getDiagnosisTag()));
                vo.setEnterpriseCount(dr.getEnterpriseCount());
                vo.setPatentCount(dr.getTotalPatentCount());
                vo.setHighValueCount(dr.getHighValuePatentCount());
            } else {
                vo.setDiagnosisTag("未知");
                vo.setDiagnosisLabel("未诊断");
                vo.setDiagnosisColor("#999999");
                vo.setEnterpriseCount(0);
                vo.setPatentCount(0);
                vo.setHighValueCount(0);
            }
            return vo;
        }).collect(Collectors.toList());
    }

    /** C03: 节点关联关系 (按产业链过滤) */
    @Override
    public List<ChainLinkResponse> getChainLinks(Long chainId) {
        List<ChainNode> chainNodes = chainNodeMapper.selectByChainId(chainId);
        if (chainNodes == null) return Collections.emptyList();
        Set<Long> chainNodeIds = chainNodes.stream()
            .map(ChainNode::getId).collect(Collectors.toSet());

        List<ChainNodeLink> allLinks = chainNodeLinkMapper.selectAll();
        List<ChainNodeLink> links = allLinks.stream()
            .filter(l -> chainNodeIds.contains(l.getSourceNodeId()) && chainNodeIds.contains(l.getTargetNodeId()))
            .collect(Collectors.toList());

        List<ChainNode> allNodes = chainNodeMapper.selectList(null);
        Map<Long, String> nameMap = allNodes.stream()
            .collect(Collectors.toMap(ChainNode::getId, ChainNode::getNodeName, (a, b) -> a));

        return links.stream().map(l -> {
            ChainLinkResponse vo = new ChainLinkResponse();
            vo.setId(l.getId());
            vo.setSourceNodeId(l.getSourceNodeId());
            vo.setSourceNodeName(nameMap.getOrDefault(l.getSourceNodeId(), ""));
            vo.setTargetNodeId(l.getTargetNodeId());
            vo.setTargetNodeName(nameMap.getOrDefault(l.getTargetNodeId(), ""));
            vo.setRelationType(l.getRelationType());
            return vo;
        }).collect(Collectors.toList());
    }

    /** C04: 节点详情 (含关联企业) */
    @Override
    public NodeDetailResponse getChainNodeDetail(Long nodeId) {
        ChainNode node = chainNodeMapper.selectById(nodeId);
        if (node == null) return null;

        NodeDetailResponse vo = new NodeDetailResponse();
        vo.setId(node.getId());
        vo.setNodeName(node.getNodeName());
        vo.setNodeLevel(layerToCode(node.getLayer()));
        vo.setNodeLevelCode(node.getLayer() != null ? node.getLayer() : "0");
        vo.setNodeLevelLabel(ChainUtils.layerToLabel(node.getLayer()));

        NodeDiagnosis dr = nodeDiagnosisMapper.selectLatestByNodeId(nodeId);
        if (dr != null) {
            vo.setDiagnosisTag(dr.getDiagnosisTag());
            vo.setDiagnosisLabel(dr.getDiagnosisTag());
            vo.setDiagnosisColor(ChainUtils.tagToColor(dr.getDiagnosisTag()));
            vo.setEnterpriseCount(dr.getEnterpriseCount());
            vo.setPatentCount(dr.getTotalPatentCount());
            vo.setHighValueCount(dr.getHighValuePatentCount());
            vo.setDescription(dr.getDiagnosisBasis());
        }

        List<Map<String, Object>> topEnts = chainNodeMapper.selectTopEnterprisesByNodeId(nodeId, 10);
        if (topEnts != null) {
            vo.setEnterprises(topEnts.stream().map(m -> {
                NodeDetailResponse.AssociatedEnterprise ae = new NodeDetailResponse.AssociatedEnterprise();
                ae.setEnterpriseId(toLong(m.get("enterprise_id")));
                ae.setEnterpriseName((String) m.get("enterprise_name"));
                ae.setPatentCount(toInt(m.get("patent_count")));
                return ae;
            }).collect(Collectors.toList()));
        }
        return vo;
    }

    /** layer (up/mid/down) → 前端编码 ("1"/"2"/"3") */
    private String layerToCode(String layer) {
        if ("up".equals(layer)) return "1";
        if ("mid".equals(layer)) return "2";
        if ("down".equals(layer)) return "3";
        return "2";
    }

    /** C05: 创建节点 */
    @Override
    @Transactional
    public void createNode(Long chainId, String nodeName, String nodeCode, String layer, Integer sortOrder) {
        ChainNode node = ChainNode.builder()
            .chainId(chainId).nodeName(nodeName).nodeCode(nodeCode)
            .layer(layer).sortOrder(sortOrder != null ? sortOrder : 0)
            .build();
        chainNodeMapper.insert(node);
    }

    /** C06: 更新节点 */
    @Override
    @Transactional
    public void updateNode(Long nodeId, String nodeName, String nodeCode, String layer, Integer sortOrder) {
        ChainNode node = chainNodeMapper.selectById(nodeId);
        if (node == null) throw new RuntimeException("Node not found: " + nodeId);
        if (nodeName != null) node.setNodeName(nodeName);
        if (nodeCode != null) node.setNodeCode(nodeCode);
        if (layer != null) node.setLayer(layer);
        if (sortOrder != null) node.setSortOrder(sortOrder);
        chainNodeMapper.updateById(node);
    }

    /** C07: 删除节点 */
    @Override
    @Transactional
    public void deleteNode(Long nodeId) {
        chainNodeMapper.deleteById(nodeId);
    }

    /** C08: 创建节点关联 */
    @Override
    @Transactional
    public void createLink(Long sourceNodeId, Long targetNodeId, String relationType) {
        ChainNodeLink link = ChainNodeLink.builder()
            .sourceNodeId(sourceNodeId).targetNodeId(targetNodeId)
            .relationType(relationType != null ? relationType : "supply")
            .build();
        chainNodeLinkMapper.insert(link);
    }

    /** C09: 删除节点关联 */
    @Override
    @Transactional
    public void deleteLink(Long linkId) {
        chainNodeLinkMapper.deleteById(linkId);
    }

    // ─── IIndustryChainService (兼容旧接口) ───
    @Override
    public com.industrybrain.dto.response.IndustryChainOverviewResponse getOverview(
            com.industrybrain.dto.request.IndustryChainQueryRequest request) {
        return new com.industrybrain.dto.response.IndustryChainOverviewResponse();
    }

    @Override
    public com.industrybrain.dto.response.IndustryChainOverviewResponse.ChainNodeVO getNodeDetail(Long nodeId) {
        return null;
    }

    @Override
    public java.util.List<com.industrybrain.dto.response.IndustryChainOverviewResponse> getAllChains() {
        return java.util.Collections.emptyList();
    }

    // ─── helpers ───
    private Long toLong(Object val) { return ChainUtils.toLong(val); }
    private int toInt(Object val) { return ChainUtils.toInt(val); }
}
