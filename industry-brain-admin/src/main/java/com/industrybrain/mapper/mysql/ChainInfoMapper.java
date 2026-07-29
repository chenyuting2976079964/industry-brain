package com.industrybrain.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.industrybrain.domain.model.IndustryChain;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 产业链目录 Mapper
 * @author 何锦鹏
 */
@Mapper
public interface ChainInfoMapper extends BaseMapper<IndustryChain> {

    /**
     * 按排序查询所有产业链
     */
    List<IndustryChain> selectAllOrdered();

    /**
     * 查询产业链统计信息 (节点数 + 企业数)
     */
    Map<String, Object> selectWithStats(@Param("chainId") Long chainId);
}
