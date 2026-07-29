package com.industrybrain.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.industrybrain.domain.model.EnterpriseAdvantage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 企业技术优势 Mapper (enterprise_advantage表)
 */
@Mapper
public interface EnterpriseAdvantageMapper extends BaseMapper<EnterpriseAdvantage> {

    /** 按企业ID查询技术优势列表（按排序序号升序） */
    @Select("SELECT id, enterprise_id, advantage_name, advantage_count, description, sort_order, created_at " +
            "FROM enterprise_advantage WHERE enterprise_id = #{enterpriseId} ORDER BY sort_order")
    List<EnterpriseAdvantage> selectByEnterpriseId(@Param("enterpriseId") Long enterpriseId);

    /** 按企业ID查询TOP N优势 */
    @Select("SELECT id, enterprise_id, advantage_name, advantage_count, description, sort_order, created_at " +
            "FROM enterprise_advantage WHERE enterprise_id = #{enterpriseId} ORDER BY sort_order LIMIT #{limit}")
    List<EnterpriseAdvantage> selectTopByEnterpriseId(@Param("enterpriseId") Long enterpriseId, @Param("limit") int limit);
}
