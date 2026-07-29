package com.industrybrain.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.industrybrain.domain.model.DataSyncLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 数据同步日志 Mapper (data_sync_log表 — DB设计v2.0表15)
 */
@Mapper
public interface DataSyncLogMapper extends BaseMapper<DataSyncLog> {

    @Select("SELECT * FROM data_sync_log WHERE sync_type = #{syncType} ORDER BY created_at DESC LIMIT 10")
    List<DataSyncLog> selectByType(@Param("syncType") String syncType);

    @Select("SELECT * FROM data_sync_log ORDER BY created_at DESC LIMIT #{limit}")
    List<DataSyncLog> selectRecent(@Param("limit") int limit);

    @Select("SELECT id, sync_type, data_source, sync_start_time, sync_end_time, " +
            "total_count, success_count, fail_count, sync_status, error_msg, created_at " +
            "FROM data_sync_log ORDER BY created_at DESC LIMIT 1")
    DataSyncLog selectLatest();
}
