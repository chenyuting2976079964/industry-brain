package com.industrybrain.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.industrybrain.domain.model.EnterpriseAlias;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 企业别名 Mapper (enterprise_alias表 — DB设计v2.0表8)
 */
@Mapper
public interface EnterpriseAliasMapper extends BaseMapper<EnterpriseAlias> {

    @Select("SELECT * FROM enterprise_alias WHERE enterprise_id = #{enterpriseId} ORDER BY id")
    List<EnterpriseAlias> selectByEnterpriseId(@Param("enterpriseId") Long enterpriseId);
}
