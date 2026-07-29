package com.industrybrain.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.industrybrain.domain.model.Alert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 预警 Mapper (alert表)
 */
@Mapper
public interface AlertMapper extends BaseMapper<Alert> {

    /** 按预警级别查询未处理预警 */
    @Select("SELECT * FROM alert WHERE alert_level = #{level} AND is_resolved = 0 AND is_deleted = 0 ORDER BY created_at DESC")
    List<Alert> selectUnresolvedByLevel(@Param("level") String level);

    /** 查询所有未处理预警 */
    @Select("SELECT * FROM alert WHERE is_resolved = 0 AND is_deleted = 0 ORDER BY created_at DESC")
    List<Alert> selectAllUnresolved();

    /** 按关联节点ID查询 */
    @Select("SELECT * FROM alert WHERE related_node_id = #{nodeId} AND is_deleted = 0 ORDER BY created_at DESC")
    List<Alert> selectByNodeId(@Param("nodeId") Long nodeId);

    /** 统计未处理数量 */
    @Select("SELECT COUNT(*) FROM alert WHERE is_resolved = 0 AND is_deleted = 0")
    int countUnresolved();

    /** 标记预警为已处理 */
    @Update("UPDATE alert SET is_resolved = 1, resolved_at = #{resolvedAt} WHERE id = #{id}")
    int resolve(@Param("id") Long id, @Param("resolvedAt") LocalDateTime resolvedAt);
}
