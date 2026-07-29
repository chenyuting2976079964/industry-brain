package com.industrybrain.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.industrybrain.domain.model.ChainNode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 产业链节点 Mapper
 * @author 何锦鹏
 */
@Mapper
public interface ChainNodeMapper extends BaseMapper<ChainNode> {

    /**
     * 按产业链ID查询所有节点
     */
    List<ChainNode> selectByChainId(@Param("chainId") Long chainId);

    /**
     * 按产业链ID和层级查询节点
     */
    List<ChainNode> selectByChainIdAndLayer(@Param("chainId") Long chainId, @Param("nodeLevel") String nodeLevel);

    /**
     * 查询某节点下专利数量最多的企业 (Top N)
     * JOIN: enterprise_info → patent_info → patent_node_mapping
     */
    List<Map<String, Object>> selectTopEnterprisesByNodeId(@Param("nodeId") Long nodeId, @Param("limit") int limit);
}
