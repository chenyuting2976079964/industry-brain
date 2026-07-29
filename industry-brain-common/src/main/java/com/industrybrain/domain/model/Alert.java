package com.industrybrain.domain.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 预警实体 (alert表)
 * @author 何锦鹏
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("alert")
public class Alert {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 预警级别：high / medium / info */
    private String alertLevel;

    /** 预警标题 */
    private String title;

    /** 预警描述 */
    private String description;

    /** 关联产业链节点ID */
    private Long relatedNodeId;

    /** 是否已处理：0-未处理 / 1-已处理 */
    private Integer isResolved;

    /** 处理时间 */
    private LocalDateTime resolvedAt;

    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;

    /** 删除时间 */
    private LocalDateTime deletedAt;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
