package com.industrybrain.domain.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 招商报告实体 (report表)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("report")
public class Report {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String reportType;

    private String scope;

    private String format;

    private Long fileId;

    private String status;

    private Long regionId;

    private Long userId;

    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;

    private LocalDateTime deletedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
