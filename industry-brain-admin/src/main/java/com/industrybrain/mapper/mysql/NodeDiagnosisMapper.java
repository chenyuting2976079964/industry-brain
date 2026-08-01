package com.industrybrain.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.industrybrain.domain.model.NodeDiagnosis;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 节点诊断结果 Mapper
 * MyBatis XML 映射: NodeDiagnosisMapper.xml
 * @author 何锦鹏
 */
@Mapper
public interface NodeDiagnosisMapper extends BaseMapper<NodeDiagnosis> {

    /** 查询每个节点每个区域最新的诊断结果 */
    List<Map<String, Object>> selectLatestPerNode();

    /** 按诊断标签统计节点数量 */
    List<Map<String, Object>> selectStatusSummary();

    /** 查询某节点最新的诊断结果 */
    NodeDiagnosis selectLatestByNodeId(@Param("nodeId") Long nodeId);

    /** 查询某节点的历史诊断记录 (按诊断日期倒序) */
    List<NodeDiagnosis> selectHistoryByNodeId(@Param("nodeId") Long nodeId);

    /** 查询指定产业链的最新诊断结果 */
    List<NodeDiagnosis> selectLatestByChainId(@Param("chainId") Long chainId);

    /** 按诊断标签筛选最新诊断结果 */
    @Select("SELECT nd.* FROM node_diagnosis nd " +
            "INNER JOIN (SELECT node_id, MAX(diagnosis_date) AS max_date " +
            "FROM node_diagnosis WHERE is_deleted = 0 GROUP BY node_id) latest " +
            "ON nd.node_id = latest.node_id AND nd.diagnosis_date = latest.max_date " +
            "WHERE nd.diagnosis_tag = #{tag} AND nd.is_deleted = 0")
    List<NodeDiagnosis> selectByTag(@Param("tag") String tag);

    /** 查询所有节点最新的诊断结果 */
    @Select("SELECT nd.* FROM node_diagnosis nd " +
            "INNER JOIN (SELECT node_id, MAX(diagnosis_date) AS max_date " +
            "FROM node_diagnosis WHERE is_deleted = 0 GROUP BY node_id) latest " +
            "ON nd.node_id = latest.node_id AND nd.diagnosis_date = latest.max_date " +
            "WHERE nd.is_deleted = 0")
    List<NodeDiagnosis> selectAllLatest();

    /** 统计被诊断的节点总数 */
    @Select("SELECT COUNT(DISTINCT node_id) FROM node_diagnosis WHERE is_deleted = 0")
    long selectNodeCount();

    /** 物理删除某节点某日某区域的诊断记录（uk_region_node_date 唯一键冲突前清理旧行） */
    @org.apache.ibatis.annotations.Delete("DELETE FROM node_diagnosis WHERE region_id = #{regionId} AND node_id = #{nodeId} AND diagnosis_date = #{diagnosisDate}")
    int physicallyDelete(@Param("regionId") Long regionId, @Param("nodeId") Long nodeId, @Param("diagnosisDate") java.time.LocalDate diagnosisDate);
}
