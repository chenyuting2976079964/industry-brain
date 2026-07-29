package com.industrybrain.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.industrybrain.domain.model.EnterpriseEvaluation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 企业招商评估结果 Mapper (enterprise_evaluation表)
 */
@Mapper
public interface EnterpriseEvaluationMapper extends BaseMapper<EnterpriseEvaluation> {

    /** 按企业ID查询评估结果 */
    @Select("SELECT * FROM enterprise_evaluation WHERE enterprise_id = #{enterpriseId}")
    EnterpriseEvaluation selectByEnterpriseId(@Param("enterpriseId") Long enterpriseId);

    /** 按评级查询企业ID列表 */
    @Select("SELECT enterprise_id FROM enterprise_evaluation WHERE eval_level = #{level}")
    List<Long> selectEnterpriseIdsByLevel(@Param("level") String level);

    /** 查询所有已评估企业的ID列表 */
    @Select("SELECT DISTINCT enterprise_id FROM enterprise_evaluation")
    List<Long> selectAllEvaluatedEnterpriseIds();

    /** 按急紧程度查询 */
    @Select("SELECT * FROM enterprise_evaluation WHERE urgency = #{urgency} ORDER BY tech_match DESC")
    List<EnterpriseEvaluation> selectByUrgency(@Param("urgency") String urgency);

    /** 更新评估结果（按企业ID） */
    @Update("UPDATE enterprise_evaluation SET eval_level = #{evalLevel}, eval_label = #{evalLabel}, " +
            "tech_match = #{techMatch}, patent_strength = #{patentStrength}, driving_potential = #{drivingPotential}, " +
            "invest_type = #{investType}, urgency = #{urgency}, risk_level = #{riskLevel}, " +
            "policy_category = #{policyCategory}, suggestion = #{suggestion}, land_suggest = #{landSuggest}, " +
            "weakness = #{weakness}, weakness_detail = #{weaknessDetail} " +
            "WHERE enterprise_id = #{enterpriseId}")
    int updateByEnterpriseId(EnterpriseEvaluation evaluation);
}
