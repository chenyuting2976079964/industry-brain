package com.industrybrain.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.industrybrain.domain.model.UserToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 用户Token Mapper (user_token表 — DB设计v2.0表6)
 */
@Mapper
public interface UserTokenMapper extends BaseMapper<UserToken> {

    @Select("SELECT * FROM user_token WHERE token = #{token}")
    UserToken selectByToken(@Param("token") String token);

    @Select("SELECT * FROM user_token WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<UserToken> selectByUserId(@Param("userId") Long userId);

    @Select("DELETE FROM user_token WHERE expire_at < NOW()")
    int deleteExpired();
}
