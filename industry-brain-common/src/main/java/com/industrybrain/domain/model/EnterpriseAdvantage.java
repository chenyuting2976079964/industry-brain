package com.industrybrain.domain.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 企业技术优势实体 (enterprise_advantage表)
 * @author 何锦鹏
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("enterprise_advantage")
public class EnterpriseAdvantage {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 企业ID */
    private Long enterpriseId;

    /** 技术优势名称 */
    private String advantageName;

    /** 相关专利数量（可为区间，如"200+"） */
    private String advantageCount;

    /** 优势描述 */
    private String description;

    /** 排序序号 */
    private Integer sortOrder;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
