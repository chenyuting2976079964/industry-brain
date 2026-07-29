/**
 * 全景大屏 API 服务 — F01~F06
 * 对接 IndustryController (后端实际路径: /api/v1/dashboard/*)
 */
import { get } from './api'

const BASE = '/dashboard'

/** F01: 底部KPI 四指标 */
export function getKpi(regionCode = '340500') {
  return get(`${BASE}/kpi?regionCode=${regionCode}`)
}

/** F02: 企业分布 5格统计 */
export function getEnterpriseDist(regionCode = '340500') {
  return get(`${BASE}/enterprise-dist?regionCode=${regionCode}`)
}

/** F03: 技术领域企业分布 (柱状图) */
export function getTechDist(regionCode = '340500') {
  return get(`${BASE}/tech-dist?regionCode=${regionCode}`)
}

/** F04: 高价值专利 TOP5 */
export function getPatentTop5(regionCode = '340500') {
  return get(`${BASE}/patent-top5?regionCode=${regionCode}`)
}

/** F05: 产业链强弱分布 (环形图) */
export function getChainStrength(regionCode = '340500') {
  return get(`${BASE}/chain-strength?regionCode=${regionCode}`)
}

/** F06: 地图标注点 */
export function getMapMarkers(regionCode = '340500') {
  return get(`${BASE}/map-markers?regionCode=${regionCode}`)
}
