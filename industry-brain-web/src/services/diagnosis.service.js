/**
 * 强弱诊断 API 服务 — D01~D04
 * 对接 DiagnosisController
 */
import { get, post } from './api'

const BASE = '/diagnosis'

/** D01: 诊断列表 */
export function getDiagnosisList(status, regionCode = '340500') {
  const params = new URLSearchParams({ regionCode })
  if (status && status !== 'all') params.set('status', status)
  return get(`${BASE}/list?${params}`)
}

/** D02: 节点诊断详情 */
export function getDiagnosisNodeDetail(nodeId) { return get(`${BASE}/node/${nodeId}`) }

/** D03: 触发重新诊断 */
export function triggerRecalc(regionCode = '340500') {
  return post(`${BASE}/recalc?regionCode=${regionCode}`)
}

/** D03: 查询诊断任务状态 */
export function getRecalcStatus(taskId) { return get(`${BASE}/recalc/status/${taskId}`) }

/** D04: 诊断汇总统计 */
export function getDiagnosisSummary(regionCode = '340500') {
  return get(`${BASE}/summary?regionCode=${regionCode}`)
}
