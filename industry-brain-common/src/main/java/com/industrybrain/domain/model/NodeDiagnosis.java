package com.industrybrain.domain.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 节点诊断结果实体 (node_diagnosis表)
 * @author 何锦鹏
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("node_diagnosis")
public class NodeDiagnosis {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 区域ID */
    private Long regionId;

    /** 产业链节点ID */
    private Long nodeId;

    /** 诊断标签: 强链/补链/弱链/缺链 */
    private String diagnosisTag;

    /** 高价值专利数量 */
    private Integer highValuePatentCount;

    /** 总专利数量 */
    private Integer totalPatentCount;

    /** 覆盖企业数量 */
    private Integer enterpriseCount;

    /** 全国该节点企业数（本地+外地） */
    private Integer nationalEnterpriseCount;

    /** 本地该节点企业数 */
    private Integer localEnterpriseCount;

    /** 本地企业全国占比(小数,0~1) */
    private java.math.BigDecimal localEnterpriseShare;

    /** 专利密集度百分比 (v2.1) */
    private java.math.BigDecimal patentDensity;

    /** 诊断依据说明 */
    private String diagnosisBasis;

    /** 诊断日期 */
    private LocalDate diagnosisDate;

    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;

    private LocalDateTime deletedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
