package com.industrybrain.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.industrybrain.domain.model.PatentNode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 专利-节点关联 Mapper (patent_node表 — DB设计v2.0表11)
 */
@Mapper
public interface PatentNodeMapper extends BaseMapper<PatentNode> {

    @Select("SELECT * FROM patent_node WHERE patent_id = #{patentId}")
    List<PatentNode> selectByPatentId(@Param("patentId") Long patentId);

    @Select("SELECT * FROM patent_node WHERE node_id = #{nodeId}")
    List<PatentNode> selectByNodeId(@Param("nodeId") Long nodeId);

    @Select("SELECT COUNT(*) FROM patent_node WHERE node_id = #{nodeId}")
    int countByNodeId(@Param("nodeId") Long nodeId);
}
