package com.industrybrain.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 产业链全景响应DTO
 */
@Data
public class IndustryChainOverviewResponse {

    /** 产业链ID */
    private Long chainId;

    /** 产业链名称 */
    private String chainName;

    /** 产业链节点列表 */
    private List<ChainNodeVO> nodes;

    /** 关联企业总数 */
    private Integer totalEnterprises;

    /** 关联专利总数 */
    private Integer totalPatents;

    /** 区域编码 */
    private String regionCode;

    /** 产业链描述 */
    private String description;

    @Data
    public static class ChainNodeVO {
        /** 节点ID */
        private Long nodeId;

        /** 节点名称 */
        private String nodeName;

        /** 层级 */
        private String level;

        /** 关联企业数 */
        private Integer enterpriseCount;

        /** 关联专利数 */
        private Integer patentCount;

        /** 子节点列表 */
        private List<ChainNodeVO> children;
    }
}
