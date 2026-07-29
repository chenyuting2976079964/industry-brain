package com.industrybrain.domain.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 产业链目录实体 (industry_chain表)
 * @author 何锦鹏
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("industry_chain")
public class IndustryChain {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 产业链名称 */
    private String name;

    /** 产业链唯一编码 */
    private String code;

    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;

    private LocalDateTime deletedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
