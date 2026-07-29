package com.industrybrain.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.industrybrain.domain.model.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 操作日志 Mapper (operation_log表 — DB设计v2.0表14)
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {

    @Select("SELECT * FROM operation_log WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT #{limit}")
    List<OperationLog> selectByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    @Select("SELECT * FROM operation_log ORDER BY created_at DESC LIMIT #{limit}")
    List<OperationLog> selectRecent(@Param("limit") int limit);
}
