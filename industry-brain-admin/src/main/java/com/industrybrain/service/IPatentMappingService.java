package com.industrybrain.service;

import com.industrybrain.domain.model.Patent;

import java.util.List;

/**
 * 【算法A1】专利→产业链节点智能映射接口
 * 三通道混合架构: 规则匹配 + 语义向量匹配(MVP占位) + 先验映射
 * @author 何锦鹏
 */
public interface IPatentMappingService {

    List<MappingResult> mapPatentToNode(Patent patent);

    default List<MappingResult> batchMapPatents(List<Patent> patents) {
        return patents.stream().flatMap(p -> mapPatentToNode(p).stream()).toList();
    }

    @lombok.Data
    class MappingResult {
        private Long patentId;
        private Long nodeId;
        private Double confidence;
        private String confidenceLevel;
        private String matchType;
        private Double ruleScore;
        private Double vectorScore;
        private Double priorScore;
        private String evidence;
    }
}
