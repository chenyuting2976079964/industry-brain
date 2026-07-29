package com.industrybrain.service;

import com.industrybrain.dto.response.*;

import java.util.List;

/**
 * 全景大屏业务接口 — F01~F06
 * 聚合 MySQL/ES/Neo4j → 数据查询 → 返回
 *
 * @author 何锦鹏
 */
public interface IDashboardService {

    /** F01: KPI 四指标 */
    DashboardKpiResponse getKpi(String regionCode);

    /** F02: 企业分布5格统计 */
    List<EnterpriseDistResponse> getEnterpriseDistribution(String regionCode);

    /** F03: 技术领域企业分布 */
    List<TechDistResponse> getTechDistribution(String regionCode);

    /** F04: 高价值专利 TOP5 */
    List<PatentTop5Response> getPatentTop5(String regionCode);

    /** F05: 产业链强弱分布 */
    List<ChainStrengthResponse> getChainStrengthDistribution(String regionCode);

    /** F06: 地图标注点 */
    List<MapMarkerResponse> getMapMarkers(String regionCode);
}
