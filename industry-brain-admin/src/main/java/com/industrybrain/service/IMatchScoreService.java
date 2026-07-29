package com.industrybrain.service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import com.industrybrain.domain.model.MatchScoreResult;

/**
 * 技术匹配度算法接口
 * 负责企业与产业链/技术的匹配度计算
 */
public interface IMatchScoreService {

    /**
     * 计算企业与产业链的技术匹配度
     */
    MatchScoreResult calculateMatch(Long enterpriseId, Long chainId);

    /**
     * 批量计算匹配度
     */
    java.util.List<MatchScoreResult> batchCalculate(java.util.List<Long> enterpriseIds, Long chainId);
}
