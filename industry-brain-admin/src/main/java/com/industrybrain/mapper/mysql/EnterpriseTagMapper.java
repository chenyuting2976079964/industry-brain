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

    /** 统计各标签的企业数量（图谱节点生成用） */
    @Select("SELECT tag_name AS tagName, COUNT(DISTINCT enterprise_id) AS enterpriseCount " +
            "FROM enterprise_tag GROUP BY tag_name ORDER BY enterpriseCount DESC")
    List<java.util.Map<String, Object>> selectTagStats();

    /** 按标签名查询企业ID列表 */
    @Select("SELECT DISTINCT enterprise_id FROM enterprise_tag WHERE tag_name = #{tagName}")
    List<Long> selectEnterpriseIdsByTagName(@Param("tagName") String tagName);

    /** 按标签名查询本地企业ID列表（is_local=1，产业链构建只用本地企业） */
    @Select("SELECT DISTINCT t.enterprise_id FROM enterprise_tag t " +
            "INNER JOIN enterprise e ON e.id = t.enterprise_id AND e.is_deleted = 0 AND e.is_local = 1 " +
            "WHERE t.tag_name = #{tagName}")
    List<Long> selectLocalEnterpriseIdsByTagName(@Param("tagName") String tagName);

    /** 标签两两共现企业数（产业链自动聚类用） */
    @Select("SELECT a.tag_name AS tagA, b.tag_name AS tagB, COUNT(DISTINCT a.enterprise_id) AS overlap " +
            "FROM enterprise_tag a JOIN enterprise_tag b " +
            "ON a.enterprise_id = b.enterprise_id AND a.tag_name < b.tag_name " +
            "GROUP BY a.tag_name, b.tag_name")
    List<java.util.Map<String, Object>> selectTagCooccurrence();
}
