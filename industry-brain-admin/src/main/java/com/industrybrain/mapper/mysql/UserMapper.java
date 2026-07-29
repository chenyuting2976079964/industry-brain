package com.industrybrain.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.industrybrain.domain.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 用户 Mapper (user表 — DB设计v2.0表5)
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    // use XML mapper with resultMap for proper column mapping
    User selectByUsername(@Param("username") String username);

    List<User> selectByRegionId(@Param("regionId") Long regionId);
}
