package com.industrybrain.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.industrybrain.domain.model.EnterpriseTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 企业技术标签 Mapper (enterprise_tag表)
 */
@Mapper
public interface EnterpriseTagMapper extends BaseMapper<EnterpriseTag> {

    /** 按企业ID查询所有技术标签 */
    @Select("SELECT id, enterprise_id, tag_name, created_at FROM enterprise_tag WHERE enterprise_id = #{enterpriseId}")
    List<EnterpriseTag> selectByEnterpriseId(@Param("enterpriseId") Long enterpriseId);

    /** 按标签名模糊搜索企业ID列表 */
    @Select("SELECT DISTINCT enterprise_id FROM enterprise_tag WHERE tag_name LIKE CONCAT('%', #{keyword}, '%')")
    List<Long> selectEnterpriseIdsByTag(@Param("keyword") String keyword);

    /** 按企业ID删除所有标签 */
    @Select("DELETE FROM enterprise_tag WHERE enterprise_id = #{enterpriseId}")
    int deleteByEnterpriseId(@Param("enterpriseId") Long enterpriseId);
}
