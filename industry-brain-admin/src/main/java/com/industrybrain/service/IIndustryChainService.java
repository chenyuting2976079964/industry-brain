package com.industrybrain.service;

import com.industrybrain.dto.request.IndustryChainQueryRequest;
import com.industrybrain.dto.response.*;

import java.util.List;

/**
 * 产业链业务接口
 * 负责产业链全景、大屏展示、节点详情、节点CRUD、关联管理等
 */
public interface IIndustryChainService {

    /** C01: 获取产业链目录列表 */
    List<ChainResponse> getChainList();

    /** C02: 获取产业链全部节点 (含四档诊断标签) */
    List<ChainNodeResponse> getChainNodes(Long chainId);

    /** C03: 获取节点关联关系 */
    List<ChainLinkResponse> getChainLinks(Long chainId);

    /** C04: 获取节点详情 (含关联企业) */
    NodeDetailResponse getChainNodeDetail(Long nodeId);

    /** C05: 创建产业链节点 */
    void createNode(Long chainId, String nodeName, String nodeCode, String layer, Integer sortOrder);

    /** C06: 更新节点信息 */
    void updateNode(Long nodeId, String nodeName, String nodeCode, String layer, Integer sortOrder);

    /** C07: 删除节点 (级联删关联) */
    void deleteNode(Long nodeId);

    /** C08: 创建节点关联 */
    void createLink(Long sourceNodeId, Long targetNodeId, String relationType);

    /** C09: 删除节点关联 */
    void deleteLink(Long linkId);

    // ─── 旧接口（保持兼容） ───

    /** 获取产业链全景概览 */
    IndustryChainOverviewResponse getOverview(IndustryChainQueryRequest request);

    /** 获取产业链节点详情（旧版） */
    IndustryChainOverviewResponse.ChainNodeVO getNodeDetail(Long nodeId);

    /** 获取所有产业链列表（旧版） */
    List<IndustryChainOverviewResponse> getAllChains();
}
