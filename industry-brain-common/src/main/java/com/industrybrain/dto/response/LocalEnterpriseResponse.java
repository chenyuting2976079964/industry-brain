package com.industrybrain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 本地企业概览响应 DTO
 * 对应原型 local-enterprises.html
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocalEnterpriseResponse {
    private Long id;
    private String name;              // 企业名称
    private String description;       // 企业简介
    private String type;              // 企业类型: 龙头企业/高新技术企业/科技型中小企业/专精特新
    private String tag;               // 标签
    private String district;          // 所在区
    private String park;              // 所属园区
    private String patentCount;       // 专利总数（含+号）
    private Integer highValueCount;   // 高价值专利数
    private Integer enterpriseCount;  // 覆盖企业数
    private String advantages;        // 技术优势
    private String techTags;          // 技术标签
    private String weaknesses;        // 技术短板
    private String registeredCapital; // 注册资本
    private String legalPerson;       // 法定代表人
}
