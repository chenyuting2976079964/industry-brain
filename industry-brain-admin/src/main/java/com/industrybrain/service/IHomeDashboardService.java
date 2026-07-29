package com.industrybrain.service;

import java.util.List;
import java.util.Map;

/**
 * 首页看板业务接口 — H01~H09
 * @author 何锦鹏
 */
public interface IHomeDashboardService {

    /** H01: KPI 三卡片数据 */
    Map<String, Object> getKpi();

    /** H02: 产业链上中下游分布 */
    Map<String, Object> getChainDist();

    /** H03: 预警列表 */
    List<Map<String, Object>> getAlerts();

    /** H04: 忽略预警 */
    void dismissAlert(Long id);

    /** H05: 园区统计 */
    List<Map<String, Object>> getParkStats();

    /** H06: 专利详情 */
    Map<String, Object> getPatentDetail();

    /** H07: 企业详情（5阶梯） */
    Map<String, Object> getEnterpriseDetail();

    /** H08: 诊断汇总 */
    Map<String, Object> getDiagnosisSummary();

    /** H09: 预警详情 */
    Map<String, Object> getAlertDetail(Long id);

    /** H10: 园区专利技术领域分布 */
    Map<String, Object> getParkPatentDetail(String parkName);

    /** H11: 园区高价值专利 TOP5 */
    List<Map<String, Object>> getParkHighValuePatents(String parkName);
}
