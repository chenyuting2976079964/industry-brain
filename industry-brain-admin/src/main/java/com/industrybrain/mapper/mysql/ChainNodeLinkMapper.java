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
    @Select("SELECT id, source_node_id, target_node_id, relation_type, is_deleted, deleted_at, created_at, updated_at " +
            "FROM chain_node_link WHERE is_deleted = 0")
    List<ChainNodeLink> selectAll();
}
