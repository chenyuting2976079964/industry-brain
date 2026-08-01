package com.industrybrain.service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import com.industrybrain.domain.model.MatchScoreResult;

/**
 * 技术匹配度算法接口
 * 负责企业与产业链/技术的匹配度计算
 */
public interface IMatchScoreService {

    /**
     * 计算企业与产业链的综合匹配度（三维度算法）
     * @param enterpriseId 企业ID
     * @param chainId 产业链节点ID
     * @param demandProvince 需求方省份编码（2位，用于地域协同度计算）
     */
    MatchScoreResult calculateMatch(Long enterpriseId, Long chainId, String demandProvince);

    /**
     * 批量计算匹配度
     * @param enterpriseIds 企业ID列表
     * @param chainId 产业链节点ID
     * @param demandProvince 需求方省份编码
     */
    java.util.List<MatchScoreResult> batchCalculate(java.util.List<Long> enterpriseIds, Long chainId, String demandProvince);
}
