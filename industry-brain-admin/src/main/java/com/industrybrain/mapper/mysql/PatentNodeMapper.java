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
     * 统计节点的高价值专利数（国知局五项标准，可落地两项）：
     * ① 战略性新兴产业的发明专利（IPC 属于战略新兴产业 + patent_type=发明）
     * ③ 维持年限超过10年的发明专利
     */
    @Select("SELECT COUNT(*) FROM patent_node pn " +
            "INNER JOIN patent p ON pn.patent_id = p.id AND p.is_deleted = 0 " +
            "WHERE pn.node_id = #{nodeId} " +
            "AND (p.maintenance_years >= 10 " +
            "     OR (p.patent_type = '发明' AND (" +
            "         LEFT(p.ipc_main,3) IN ('G06','H04','H01','G16','G05') " +   // 新一代信息技术
            "         OR LEFT(p.ipc_main,3) IN ('B23','B24','B25','B26','B65','B66','F01','F02','F03','F04','F15','F16') " +  // 高端装备
            "         OR LEFT(p.ipc_main,3) IN ('C01','C03','C04','C08','C09','C22','C23','C30','C07','C10') " +  // 新材料
            "         OR LEFT(p.ipc_main,3) IN ('A61','C12') " +  // 生物医药
            "         OR LEFT(p.ipc_main,3) IN ('B60','B62') " +  // 新能源汽车
            "         OR LEFT(p.ipc_main,3) IN ('H02','F03') " +  // 新能源
            "         OR LEFT(p.ipc_main,3) IN ('B01','B02','B03','B09','C02') " +  // 节能环保
            "         OR LEFT(p.ipc_main,3) IN ('G01') " +  // 新一代信息技术（检测）
            "     )))")
    int countHighValueByNodeId(@Param("nodeId") Long nodeId);

    @Select("SELECT pn.* FROM patent_node pn " +
            "INNER JOIN patent p ON pn.patent_id = p.id AND p.is_deleted = 0 " +
            "WHERE p.applicant_enterprise_id = #{enterpriseId}")
    List<PatentNode> selectByEnterpriseId(@Param("enterpriseId") Long enterpriseId);

    /** 批量插入专利-节点关联（图谱构建用） */
    int batchInsert(@Param("list") List<PatentNode> list);
}
