package com.industrybrain.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.industrybrain.domain.model.IndustryChainNode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 产业链节点 Mapper (业务对象版本，映射 chain_node 表)
 */
@Mapper
public interface IndustryChainNodeMapper extends BaseMapper<IndustryChainNode> {

    @Select("SELECT id, name AS node_name, layer AS level, parent_id, chain_id, sort_order, " +
            "description, 0 AS enterprise_count, 0 AS patent_count, " +
            "CASE WHEN is_deleted = 1 THEN 'deleted' ELSE 'active' END AS status " +
            "FROM chain_node WHERE id = #{id}")
    IndustryChainNode selectById(@Param("id") Long id);

    @Select("SELECT id, name AS node_name, layer AS level, parent_id, chain_id, sort_order, " +
            "description, 0 AS enterprise_count, 0 AS patent_count, " +
            "CASE WHEN is_deleted = 1 THEN 'deleted' ELSE 'active' END AS status " +
            "FROM chain_node WHERE chain_id = #{chainId} ORDER BY sort_order")
    List<IndustryChainNode> selectByChainId(@Param("chainId") Long chainId);

    @Select("SELECT id, name AS node_name, layer AS level, parent_id, chain_id, sort_order, " +
            "description, 0 AS enterprise_count, 0 AS patent_count, " +
            "CASE WHEN is_deleted = 1 THEN 'deleted' ELSE 'active' END AS status " +
            "FROM chain_node ORDER BY chain_id, sort_order")
    List<IndustryChainNode> selectAll();
}
