package com.industrybrain.domain.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * IPC分类→产业链节点映射规则 (ipc_chain_mapping表)
 * A1算法规则通道第1层: IPC精确/前缀匹配
 * @author Developer #2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ipc_chain_mapping")
public class IpcChainMapping {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** IPC分类号 (支持前缀匹配) */
    private String ipcCode;

    /** IPC级别: GROUP/SUB_CLASS/CLASS/SECTION */
    private String ipcLevel;

    /** 映射的目标节点ID */
    private Long nodeId;

    /** 映射权重 0~1.0 */
    private Double weight;

    /** 规则描述 */
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
