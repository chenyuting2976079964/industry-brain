package com.industrybrain.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.industrybrain.domain.model.Attachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 附件 Mapper (attachment表 — DB设计v2.0表16)
 */
@Mapper
public interface AttachmentMapper extends BaseMapper<Attachment> {

    @Select("SELECT * FROM attachment WHERE ref_type = #{refType} AND ref_id = #{refId} AND is_deleted = 0 ORDER BY created_at DESC")
    List<Attachment> selectByRef(@Param("refType") String refType, @Param("refId") Long refId);
}
