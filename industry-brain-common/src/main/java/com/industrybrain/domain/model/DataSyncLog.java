package com.industrybrain.domain.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 数据同步日志实体 (data_sync_log表)
 * @author 何锦鹏
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("data_sync_log")
public class DataSyncLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 同步类型 */
    private String syncType;

    /** 数据来源 */
    private String dataSource;

    /** 同步开始时间 */
    private LocalDateTime syncStartTime;

    /** 同步结束时间 */
    private LocalDateTime syncEndTime;

    /** 总记录数 */
    private Integer totalCount;

    /** 成功数 */
    private Integer successCount;

    /** 失败数 */
    private Integer failCount;

    /** 同步状态 */
    private String syncStatus;

    /** 错误信息 */
    private String errorMsg;

    private LocalDateTime createdAt;
}
