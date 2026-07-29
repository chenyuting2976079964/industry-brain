package com.industrybrain.domain.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 区域实体 (region表)
 * @author 何锦鹏
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("region")
public class Region {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 区域名称 */
    private String name;

    /** 区域编码 (GB/T 2260国家标准) */
    private String code;

    /** 级别：省/市/区县 */
    private String level;

    /** 父级区域ID */
    private Long parentId;

    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;

    private LocalDateTime deletedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
