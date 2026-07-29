package com.industrybrain.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.industrybrain.domain.model.Region;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 区域 Mapper (region表 — DB设计v2.0表1)
 */
@Mapper
public interface RegionMapper extends BaseMapper<Region> {

    @Select("SELECT * FROM region WHERE code = #{code} AND is_deleted = 0")
    Region selectByCode(@Param("code") String code);

    @Select("SELECT * FROM region WHERE parent_id = #{parentId} AND is_deleted = 0 ORDER BY id")
    List<Region> selectByParentId(@Param("parentId") Long parentId);

    @Select("SELECT * FROM region WHERE name = #{name} AND is_deleted = 0 LIMIT 1")
    Region selectByName(@Param("name") String name);
}
