package com.industrybrain.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.industrybrain.domain.model.Patent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 专利信息 Mapper
 * @author 何锦鹏
 */
@Mapper
public interface PatentMapper extends BaseMapper<Patent> {

    /** 按专利号查询 */
    @Select("SELECT * FROM patent WHERE patent_number = #{patentNumber} AND is_deleted = 0 LIMIT 1")
    Patent selectByPatentNumber(@Param("patentNumber") String patentNumber);

    /** 分页查询某企业的专利 (支持关键词模糊搜索和类型筛选) */
    List<Patent> selectPageByEnterprise(@Param("enterpriseId") Long enterpriseId,
                                         @Param("keyword") String keyword,
                                         @Param("patentType") String patentType,
                                         @Param("offset") int offset,
                                         @Param("limit") int limit);

    /** 分页查询某企业的专利 — 别名: selectByEnterprise */
    @Select("SELECT * FROM patent WHERE applicant_enterprise_id = #{enterpriseId} " +
            "AND is_deleted = 0 ORDER BY application_date DESC LIMIT #{offset}, #{limit}")
    List<Patent> selectByEnterprise(@Param("enterpriseId") Long enterpriseId,
                                     @Param("keyword") String keyword,
                                     @Param("patentType") String patentType,
                                     @Param("offset") int offset,
                                     @Param("limit") int limit);

    /** 统计某企业的专利总数 — 别名: countByEnterprise */
    @Select("SELECT COUNT(*) FROM patent WHERE applicant_enterprise_id = #{enterpriseId} AND is_deleted = 0")
    long countByEnterprise(@Param("enterpriseId") Long enterpriseId,
                           @Param("keyword") String keyword,
                           @Param("patentType") String patentType);

    /** 统计某企业的专利总数 */
    long selectTotalCountByEnterprise(@Param("enterpriseId") Long enterpriseId);

    /** 统计全部专利总数 (无参版本) */
    @Select("SELECT COUNT(*) FROM patent WHERE is_deleted = 0")
    long selectTotalCount();

    /** 统计某企业的高价值专利数量 */
    long selectHighValueCountByEnterprise(@Param("enterpriseId") Long enterpriseId);

    /** 统计全部高价值专利数量 (无参版本) — 国知局标准：发明 + (维持≥5年 或 战略新兴产业IPC) */
    @Select("<script>SELECT COUNT(*) FROM patent WHERE is_deleted = 0 " +
            "AND patent_type = '发明' " +
            "AND (maintenance_years >= 5 " +
            "     OR LEFT(ipc_main,3) IN (${@com.industrybrain.common.utils.HighValuePatentUtils@IPC_SQL_IN}))" +
            "</script>")
    long selectHighValueCount();


    /** 查询某企业的高价值专利 Top 5 (带参版本) */
    List<Patent> selectTop5HighValueByEnterprise(@Param("enterpriseId") Long enterpriseId);

    /** 查询全局高价值专利 Top 5 (无参版本) — 国知局标准过滤 */
    @Select("<script>SELECT id, patent_number, patent_name, applicant, maintenance_years FROM patent " +
            "WHERE is_deleted = 0 " +
            "AND patent_type = '发明' " +
            "AND (maintenance_years >= 5 " +
            "     OR LEFT(ipc_main,3) IN (${@com.industrybrain.common.utils.HighValuePatentUtils@IPC_SQL_IN})) " +
            "ORDER BY maintenance_years DESC LIMIT 5" +
            "</script>")
    List<Map<String, Object>> selectTop5HighValue();

    /** 按技术领域统计专利分布 (带参版本) */
    List<Map<String, Object>> selectTechDistributionByEnterprise(@Param("enterpriseId") Long enterpriseId);

    /** 按技术领域统计专利分布 (无参版本) */
    @Select("SELECT ipc_main AS techField, COUNT(DISTINCT applicant_enterprise_id) AS enterpriseCount " +
            "FROM patent WHERE is_deleted = 0 GROUP BY ipc_main ORDER BY enterpriseCount DESC LIMIT 20")
    List<Map<String, Object>> selectTechDistribution();

    /** 查询某节点下专利持有量最多的企业 (Top N) */
    List<Map<String, Object>> selectTopHoldersByNodeId(@Param("nodeId") Long nodeId, @Param("limit") int limit);

    /** 查询覆盖企业数 (不重复) */
    @Select("SELECT COUNT(DISTINCT applicant_enterprise_id) FROM patent WHERE is_deleted = 0")
    long selectDistinctEnterpriseCount();

    /** 按节点ID统计专利总数 */
    @Select("SELECT COUNT(*) FROM patent_node pn INNER JOIN patent p ON pn.patent_id = p.id " +
            "WHERE pn.node_id = #{nodeId} AND p.is_deleted = 0")
    int selectCountByNodeId(@Param("nodeId") Long nodeId);

    /** 按节点ID统计高价值专利数 — 国知局标准：发明 + (维持≥5年 或 战略新兴产业IPC) */
    @Select("<script>SELECT COUNT(*) FROM patent_node pn INNER JOIN patent p ON pn.patent_id = p.id " +
            "WHERE pn.node_id = #{nodeId} AND p.is_deleted = 0 " +
            "AND p.patent_type = '发明' " +
            "AND (p.maintenance_years >= 5 " +
            "     OR LEFT(p.ipc_main,3) IN (${@com.industrybrain.common.utils.HighValuePatentUtils@IPC_SQL_IN}))" +
            "</script>")
    int selectHighValueCountByNodeId(@Param("nodeId") Long nodeId);

    /** 按节点ID统计企业数 */
    @Select("SELECT COUNT(DISTINCT en.enterprise_id) FROM enterprise_node en " +
            "WHERE en.node_id = #{nodeId}")
    int selectEnterpriseCountByNodeId(@Param("nodeId") Long nodeId);

    /** 按企业ID统计专利总数 */
    @Select("SELECT COUNT(*) FROM patent WHERE applicant_enterprise_id = #{enterpriseId} AND is_deleted = 0")
    int selectCountByEnterpriseId(@Param("enterpriseId") Long enterpriseId);

    /** 按企业ID统计高价值专利数 — 国知局标准：发明 + (维持≥5年 或 战略新兴产业IPC) */
    @Select("<script>SELECT COUNT(*) FROM patent WHERE applicant_enterprise_id = #{enterpriseId} " +
            "AND is_deleted = 0 " +
            "AND patent_type = '发明' " +
            "AND (maintenance_years >= 5 " +
            "     OR LEFT(ipc_main,3) IN (${@com.industrybrain.common.utils.HighValuePatentUtils@IPC_SQL_IN}))" +
            "</script>")
    int selectHighValueCountByEnterpriseId(@Param("enterpriseId") Long enterpriseId);

    /** 按企业ID统计发明专利数 */
    @Select("SELECT COUNT(*) FROM patent WHERE applicant_enterprise_id = #{enterpriseId} " +
            "AND patent_type = '发明' AND is_deleted = 0")
    int selectInventionCountByEnterpriseId(@Param("enterpriseId") Long enterpriseId);

    /** 按专利类型统计数量 */
    @Select("SELECT COUNT(*) FROM patent WHERE patent_type = #{type} AND is_deleted = 0")
    long selectCountByType(@Param("type") String type);

    /** 按企业ID查询所有专利 — 别名 */
    @Select("SELECT * FROM patent WHERE applicant_enterprise_id = #{enterpriseId} AND is_deleted = 0 ORDER BY application_date DESC")
    List<Patent> selectByEnterpriseId(@Param("enterpriseId") Long enterpriseId);

    /** 按企业ID统计发明专利数 — 别名 */
    @Select("SELECT COUNT(*) FROM patent WHERE applicant_enterprise_id = #{enterpriseId} AND patent_type = '发明' AND is_deleted = 0")
    int selectInventionCount(@Param("enterpriseId") Long enterpriseId);

    // ==================== A3/A4/A5 算法新增方法 ====================

    /**
     * 统计企业专利总数
     */
    int countByEnterpriseId(@Param("enterpriseId") Long enterpriseId);

    /**
     * 按IPC大类聚合企业专利分布
     */
    List<java.util.Map<String, Object>> selectIpcDistribution(@Param("enterpriseId") Long enterpriseId);

    /**
     * 按企业ID集合统计 IPC subclass 级分布（链内节点生成用）
     */
    @Select("<script>SELECT LEFT(ipc_main, 4) AS ipcSubclass, COUNT(*) AS cnt, " +
            "COUNT(DISTINCT applicant_enterprise_id) AS entCount " +
            "FROM patent WHERE ipc_main IS NOT NULL AND is_deleted = 0 " +
            "AND applicant_enterprise_id IS NOT NULL " +
            "AND applicant_enterprise_id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "GROUP BY LEFT(ipc_main, 4) ORDER BY cnt DESC</script>")
    List<java.util.Map<String, Object>> selectIpcSubclassStatsByEnterpriseIds(@Param("ids") List<Long> ids);

    /**
     * 统计全部专利的IPC主分类分布（图谱IPC映射规则填充用）
     */
    @Select("SELECT ipc_main AS ipcMain, COUNT(*) AS cnt FROM patent " +
            "WHERE ipc_main IS NOT NULL AND is_deleted = 0 " +
            "GROUP BY ipc_main ORDER BY cnt DESC")
    List<java.util.Map<String, Object>> selectIpcStats();

    /**
     * IPC subclass 级分布（LEFT 4 位），含企业数（产业链自动聚合用）
     */
    @Select("SELECT LEFT(ipc_main, 4) AS ipcSubclass, COUNT(*) AS cnt, " +
            "COUNT(DISTINCT applicant_enterprise_id) AS entCount " +
            "FROM patent WHERE ipc_main IS NOT NULL AND is_deleted = 0 " +
            "AND applicant_enterprise_id IS NOT NULL " +
            "GROUP BY LEFT(ipc_main, 4) ORDER BY cnt DESC")
    List<java.util.Map<String, Object>> selectIpcSubclassStats();

    /**
     * IPC subclass 归属的企业ID列表（节点企业归属用）
     */
    @Select("SELECT DISTINCT applicant_enterprise_id FROM patent " +
            "WHERE ipc_main IS NOT NULL AND is_deleted = 0 " +
            "AND applicant_enterprise_id IS NOT NULL " +
            "AND LEFT(ipc_main, 4) = #{ipcSubclass}")
    List<Long> selectEnterpriseIdsByIpcSubclass(@Param("ipcSubclass") String ipcSubclass);

    /**
     * IPC subclass 两两共现企业数（社区发现聚类用）
     * 返回 a/b/overlap
     */
    @Select("SELECT a.ipc AS a, b.ipc AS b, COUNT(DISTINCT a.eid) AS overlap FROM ( " +
            "  SELECT LEFT(p1.ipc_main,4) AS ipc, p1.applicant_enterprise_id AS eid " +
            "  FROM patent p1 WHERE p1.ipc_main IS NOT NULL AND p1.applicant_enterprise_id IS NOT NULL AND p1.is_deleted = 0 " +
            "  GROUP BY ipc, eid " +
            ") a JOIN ( " +
            "  SELECT LEFT(p2.ipc_main,4) AS ipc, p2.applicant_enterprise_id AS eid " +
            "  FROM patent p2 WHERE p2.ipc_main IS NOT NULL AND p2.applicant_enterprise_id IS NOT NULL AND p2.is_deleted = 0 " +
            "  GROUP BY ipc, eid " +
            ") b ON a.eid = b.eid AND a.ipc < b.ipc " +
            "GROUP BY a.ipc, b.ipc")
    List<java.util.Map<String, Object>> selectIpcSubclassCooccurrence();

    /** 按企业ID集合查询专利（节点专利归属用） */
    @Select("<script>SELECT * FROM patent WHERE is_deleted = 0 " +
            "AND applicant_enterprise_id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<Patent> selectByEnterpriseIds(@Param("ids") List<Long> ids);

    /**
     * 统计企业高价值专利数（发明类型 + 被引用>=5次）— 别名: selectHighValueCountForAlgorithm
     */
    int selectHighValueCountForAlgorithm(@Param("enterpriseId") Long enterpriseId);

    /**
     * 统计企业发明专利数 — 别名: selectInventionCountForAlgorithm
     */
    int selectInventionCountForAlgorithm(@Param("enterpriseId") Long enterpriseId);

    /**
     * 统计特定IPC分类下企业的专利数
     */
    int countByIpcAndEnterprise(@Param("ipcCode") String ipcCode, @Param("enterpriseId") Long enterpriseId);

    /**
     * 查询企业近3年新增的IPC分类
     */
    List<String> selectRecentIpcCodes(@Param("enterpriseId") Long enterpriseId, @Param("sinceYear") String sinceYear);

    /**
     * 查询所有企业中最大专利数（用于归一化）
     */
    int selectMaxPatentCount();

    /**
     * 统计同行业企业在特定IPC分类下的专利数
     */
    List<java.util.Map<String, Object>> selectIndustryBenchmarkIpc(@Param("industry") String industry, @Param("limit") Integer limit);

    /**
     * 统计同行业企业平均专利数（用于基准对比）
     */
    java.util.Map<String, Object> selectIndustryPatentAvg(@Param("industry") String industry);
}
