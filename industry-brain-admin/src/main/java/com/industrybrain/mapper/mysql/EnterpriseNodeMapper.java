package com.industrybrain.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.industrybrain.domain.model.EnterpriseNode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 企业-节点关联 Mapper (enterprise_node表 — DB设计v2.0表10)
 */
@Mapper
public interface EnterpriseNodeMapper extends BaseMapper<EnterpriseNode> {

    @Select("SELECT * FROM enterprise_node WHERE enterprise_id = #{enterpriseId}")
    List<EnterpriseNode> selectByEnterpriseId(@Param("enterpriseId") Long enterpriseId);

    @Select("SELECT * FROM enterprise_node WHERE node_id = #{nodeId}")
    List<EnterpriseNode> selectByNodeId(@Param("nodeId") Long nodeId);

    @Select("SELECT COUNT(*) FROM enterprise_node WHERE node_id = #{nodeId}")
    int countByNodeId(@Param("nodeId") Long nodeId);

    /** 批量插入企业-节点关联（图谱构建用） */
    int batchInsert(@Param("list") List<EnterpriseNode> list);
}
