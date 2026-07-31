package com.industrybrain.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.industrybrain.domain.model.Enterprise;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 企业信息 Mapper
 * @author 何锦鹏
 */
@Mapper
public interface     EnterpriseMapper extends BaseMapper<Enterprise> {

    /** 查询企业并附带专利数量统计 */
    List<Map<String, Object>> selectWithPatentCount();

    /** 按企业名称模糊搜索 (单参数) */
    List<Enterprise> searchByName(@Param("keyword") String keyword);

    /** 按企业名称模糊搜索 (带limit) */
    @Select("SELECT * FROM enterprise WHERE name LIKE CONCAT('%', #{keyword}, '%') AND is_deleted = 0 LIMIT #{limit}")
    List<Enterprise> searchByName(@Param("keyword") String keyword, @Param("limit") int limit);

    /** 按区域编码查询企业列表 */
    @Select("SELECT e.id, e.name, e.unified_code, e.legal_representative, e.registered_capital, " +
            "e.establishment_date, e.business_status, e.registered_address, " +
            "e.region_id, e.is_local, e.external_id, e.contact_phone, e.data_version, " +
            "e.created_at, e.updated_at " +
            "FROM enterprise e INNER JOIN region r ON e.region_id = r.id " +
            "WHERE r.code = #{regionCode} AND e.is_deleted = 0 ORDER BY e.name ASC")
    List<Enterprise> selectByRegionCode(@Param("regionCode") String regionCode);

    /** 按企业ID查询IPC分布（带中文领域名，优先精确匹配后降级前缀匹配） */
    @Select("SELECT p.ipc_main, COUNT(*) AS cnt, " +
            "COALESCE(m1.label, m2.label, p.ipc_main) AS field_name " +
            "FROM patent p " +
            "LEFT JOIN (SELECT icm.ipc_code, cn.name AS label FROM ipc_chain_mapping icm " +
            "  INNER JOIN chain_node cn ON cn.id = icm.node_id AND cn.is_deleted = 0) m1 " +
            "  ON m1.ipc_code = p.ipc_main " +
            "LEFT JOIN (SELECT icm.ipc_code, cn.name AS label FROM ipc_chain_mapping icm " +
            "  INNER JOIN chain_node cn ON cn.id = icm.node_id AND cn.is_deleted = 0) m2 " +
            "  ON m2.ipc_code = SUBSTRING(p.ipc_main, 1, 4) " +
            "WHERE p.applicant_enterprise_id = #{enterpriseId} AND p.is_deleted = 0 " +
            "GROUP BY p.ipc_main, field_name " +
            "ORDER BY cnt DESC")
    List<Map<String, Object>> selectIpcDistribution(@Param("enterpriseId") Long enterpriseId);

    /** 按区域ID统计企业数 */
    @Select("SELECT COUNT(*) FROM enterprise WHERE region_id = #{regionId} AND is_deleted = 0")
    long selectCountByRegion(@Param("regionId") long regionId);

    /** 按是否本地企业统计 */
    @Select("SELECT COUNT(*) FROM enterprise WHERE is_local = #{isLocal} AND is_deleted = 0")
    long selectCountByLocal(@Param("isLocal") int isLocal);

    /** 按统一社会信用代码查询企业 */
    @Select("SELECT * FROM enterprise WHERE unified_code = #{unifiedCode} AND is_deleted = 0 LIMIT 1")
    Enterprise selectByUnifiedCode(@Param("unifiedCode") String unifiedCode);

    /** 本地企业查询（含专利统计） */
    @Select("<script>" +
            "SELECT e.id, e.name, e.legal_representative, e.registered_capital, " +
            "e.business_status, e.registered_address, e.is_local, e.region_id, " +
            "e.enterprise_type, r.name AS district_name, " +
            "COALESCE(ps.patent_count, 0) AS patent_count, " +
            "COALESCE(ps.high_value_count, 0) AS high_value_count, " +
            "COALESCE(ps.enterprise_count, 0) AS enterprise_count " +
            "FROM enterprise e " +
            "LEFT JOIN region r ON e.region_id = r.id " +
            "LEFT JOIN (SELECT applicant_enterprise_id, COUNT(*) AS patent_count, " +
            "SUM(CASE WHEN maintenance_years >= 5 THEN 1 ELSE 0 END) AS high_value_count, " +
            "COUNT(DISTINCT applicant_enterprise_id) AS enterprise_count " +
            "FROM patent WHERE is_deleted = 0 GROUP BY applicant_enterprise_id) ps " +
            "ON e.id = ps.applicant_enterprise_id " +
            "WHERE e.is_deleted = 0 AND e.is_local = 1 " +
            "<if test='keyword != null and keyword != \"\"'>AND e.name LIKE CONCAT('%', #{keyword}, '%')</if> " +
            "ORDER BY ps.high_value_count DESC, e.name ASC " +
            "LIMIT 200" +
            "</script>")
    List<java.util.Map<String, Object>> selectLocalEnterprises(@Param("keyword") String keyword);

    /** 按企业类型查询企业列表 */
    @Select("SELECT * FROM enterprise WHERE enterprise_type = #{type} AND is_deleted = 0")
    List<Enterprise> selectByType(@Param("type") String type);

    /** 按企业类型统计企业数 */
    @Select("SELECT COUNT(*) FROM enterprise WHERE enterprise_type = #{type} AND is_deleted = 0")
    Long selectCountByType(@Param("type") String type);

    /** 获取企业类型统计总数（所有非空类型） */
    @Select("SELECT enterprise_type, COUNT(*) as count FROM enterprise WHERE is_deleted = 0 AND enterprise_type IS NOT NULL GROUP BY enterprise_type")
    List<Map<String, Object>> selectTypeStats();

    /** 查询规模企业 ID 列表（员工规模 中型/中大型/大型）— 产业链存在性判定用 */
    @Select("SELECT id FROM enterprise WHERE is_deleted = 0 "
            + "AND employee_scale IN ('中型','中大型','大型')")
    List<Long> selectScaleEnterpriseIds();
}
