package com.industrybrain.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.industrybrain.domain.model.PatentNodeMapping;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 专利-节点映射 Mapper
 * @author Developer #2
 */
@Mapper
public interface PatentNodeMappingMapper extends BaseMapper<PatentNodeMapping> {

    /**
     * 按专利ID查询映射记录
     */
    @Select("SELECT id, patent_id, node_id, mapping_confidence, confidence_level, match_type, " +
            "rule_score, vector_score, `primary`, evidence, create_time " +
            "FROM patent_node_mapping WHERE patent_id = #{patentId}")
    List<PatentNodeMapping> selectByPatentId(@Param("patentId") Long patentId);

    /**
     * 按专利ID查询高置信度映射记录
     */
    @Select("SELECT id, patent_id, node_id, mapping_confidence, confidence_level, match_type, " +
            "rule_score, vector_score, `primary`, evidence, create_time " +
            "FROM patent_node_mapping WHERE patent_id = #{patentId} AND confidence_level = 'HIGH'")
    List<PatentNodeMapping> selectHighConfidenceByPatentId(@Param("patentId") Long patentId);

    /**
     * 按企业ID查询该企业所有专利的映射记录 (A1 申请人层依赖)
     */
    @Select("SELECT pnm.id, pnm.patent_id, pnm.node_id, pnm.mapping_confidence, pnm.confidence_level, " +
            "pnm.match_type, pnm.rule_score, pnm.vector_score, pnm.`primary`, pnm.evidence, pnm.create_time " +
            "FROM patent_node_mapping pnm " +
            "INNER JOIN patent p ON pnm.patent_id = p.id AND p.is_deleted = 0 " +
            "WHERE p.applicant_enterprise_id = #{enterpriseId}")
    List<PatentNodeMapping> selectByEnterpriseId(@Param("enterpriseId") Long enterpriseId);
}
