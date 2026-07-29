package com.industrybrain.domain.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 操作日志实体 (operation_log表)
 * @author 何锦鹏
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("operation_log")
public class OperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 区域ID */
    private Long regionId;

    /** 操作类型 */
    private String operationType;

    /** 操作描述 */
    private String operationDesc;

    /** IP地址 */
    private String ipAddress;

    /** 状态 */
    private String status;

    private LocalDateTime createdAt;
}
