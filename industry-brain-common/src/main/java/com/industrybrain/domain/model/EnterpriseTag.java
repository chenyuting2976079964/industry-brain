package com.industrybrain.domain.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 企业技术标签实体 (enterprise_tag表)
 * @author 何锦鹏
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("enterprise_tag")
public class EnterpriseTag {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 企业ID */
    private Long enterpriseId;

    /** 技术标签名称 */
    private String tagName;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
