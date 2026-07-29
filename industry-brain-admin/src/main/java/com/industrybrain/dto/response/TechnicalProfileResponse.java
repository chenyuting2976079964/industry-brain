package com.industrybrain.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 技术画像响应DTO
 */
@Data
public class TechnicalProfileResponse {

    /** 企业ID */
    private Long enterpriseId;

    /** 企业名称 */
    private String enterpriseName;

    /** 核心技术领域列表 */
    private List<TechnologyField> technologyFields;

    /** 专利总数 */
    private Integer totalPatents;

    /** 有效专利数 */
    private Integer validPatents;

    /** 研发能力评级（A/B/C/D） */
    private String rdCapabilityLevel;

    /** 技术新颖度评分（0-100） */
    private BigDecimal noveltyScore;

    /** 技术影响力评分（0-100） */
    private BigDecimal influenceScore;

    /** 技术优势领域（A4 Step3输出） */
    private List<AdvantageField> advantages;

    /** 技术短板（A4 Step4输出） */
    private List<String> weaknesses;

    /** 研发方向标签（A4 Step5输出） */
    private List<String> rdDirections;

    @Data
    public static class TechnologyField {
        /** 技术领域名称 */
        private String fieldName;

        /** IPC分类 */
        private String ipcCode;

        /** 专利数量 */
        private Integer patentCount;

        /** 最新专利日期 */
        private String latestPatentDate;
    }

    @Data
    public static class AdvantageField {
        /** 优势领域名称 */
        private String fieldName;

        /** IPC分类 */
        private String ipcCode;

        /** 企业专利数 */
        private Integer enterprisePatentCount;

        /** 行业均值 */
        private Double industryAvg;

        /** 优势倍数 */
        private Double ratio;
    }
}
