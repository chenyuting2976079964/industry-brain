package com.industrybrain.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.industrybrain.domain.model.IndustrialPark;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface IndustrialParkMapper extends BaseMapper<IndustrialPark> {
    @Select("SELECT * FROM industrial_park WHERE region_id = #{regionId} AND is_deleted = 0 ORDER BY name")
    List<IndustrialPark> selectByRegionId(@Param("regionId") Long regionId);
}
