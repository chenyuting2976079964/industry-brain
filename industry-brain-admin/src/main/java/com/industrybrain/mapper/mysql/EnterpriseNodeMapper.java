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

    /** 统计节点下大规模企业数（员工规模 大型/中大型）— 强弱链诊断判定用 */
    @Select("SELECT COUNT(DISTINCT e.id) FROM enterprise_node en " +
            "INNER JOIN enterprise e ON e.id = en.enterprise_id AND e.is_deleted = 0 " +
            "WHERE en.node_id = #{nodeId} AND e.employee_scale IN ('大型', '中大型')")
    int countLargeScaleByNodeId(@Param("nodeId") Long nodeId);

    /** 统计各层（up/mid/down）节点关联的去重企业数 — 产业链分布看板用 */
    @Select("SELECT cn.layer AS layer, COUNT(DISTINCT en.enterprise_id) AS ent_cnt " +
            "FROM enterprise_node en " +
            "INNER JOIN chain_node cn ON cn.id = en.node_id AND cn.is_deleted = 0 " +
            "WHERE cn.layer IS NOT NULL " +
            "GROUP BY cn.layer")
    List<java.util.Map<String, Object>> countByLayer();

    /** 批量插入企业-节点关联（图谱构建用） */
    int batchInsert(@Param("list") List<EnterpriseNode> list);
}
