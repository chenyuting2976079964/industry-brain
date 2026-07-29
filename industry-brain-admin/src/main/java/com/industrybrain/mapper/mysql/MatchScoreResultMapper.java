package com.industrybrain.mapper.mysql;

import com.industrybrain.domain.model.MatchScoreResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 匹配度结果 Mapper
 */
@Mapper
public interface MatchScoreResultMapper {

    MatchScoreResult selectByEnterpriseAndChain(@Param("enterpriseId") Long enterpriseId,
                                                 @Param("chainId") Long chainId);

    int insert(MatchScoreResult result);

    int update(MatchScoreResult result);

    int batchInsert(@Param("list") List<MatchScoreResult> results);
}
