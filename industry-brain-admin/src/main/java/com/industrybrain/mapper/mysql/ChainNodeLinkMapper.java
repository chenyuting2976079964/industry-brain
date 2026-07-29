package com.industrybrain.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.industrybrain.domain.model.ChainNodeLink;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 产业链节点关联关系 Mapper
 * @author 何锦鹏
 */
@Mapper
public interface ChainNodeLinkMapper extends BaseMapper<ChainNodeLink> {

    /** 查询所有关联关系 */
    @Select("SELECT id, from_node_id, to_node_id, relation_type, created_at " +
            "FROM chain_node_link")
    List<ChainNodeLink> selectAll();
}
