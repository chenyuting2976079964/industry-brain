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

    /**
     * 统计节点的高价值专利数（国知局标准）：
     * ① 战略性新兴产业的发明专利（IPC 属于战略新兴产业 + patent_type=发明）
     * ③ 维持年限超过5年的发明专利
     */
    @Select("<script>SELECT COUNT(*) FROM patent_node pn " +
            "INNER JOIN patent p ON pn.patent_id = p.id AND p.is_deleted = 0 " +
            "WHERE pn.node_id = #{nodeId} " +
            "AND p.patent_type = '发明' " +
            "AND (p.maintenance_years >= 5 " +
            "     OR LEFT(p.ipc_main,3) IN (${@com.industrybrain.common.utils.HighValuePatentUtils@IPC_SQL_IN}))" +
            "</script>")
    int countHighValueByNodeId(@Param("nodeId") Long nodeId);

    @Select("SELECT pn.* FROM patent_node pn " +
            "INNER JOIN patent p ON pn.patent_id = p.id AND p.is_deleted = 0 " +
            "WHERE p.applicant_enterprise_id = #{enterpriseId}")
    List<PatentNode> selectByEnterpriseId(@Param("enterpriseId") Long enterpriseId);

    /** 批量插入专利-节点关联（图谱构建用） */
    int batchInsert(@Param("list") List<PatentNode> list);
}
