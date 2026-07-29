package com.industrybrain.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.industrybrain.domain.model.IpcChainMapping;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * IPC分类→产业链节点映射规则 Mapper
 * @author Developer #2
 */
@Mapper
public interface IpcChainMappingMapper extends BaseMapper<IpcChainMapping> {

    /**
     * 按IPC分类号前缀匹配查询映射规则
     */
    List<IpcChainMapping> selectByIpcPrefix(@Param("ipcCode") String ipcCode);
}
