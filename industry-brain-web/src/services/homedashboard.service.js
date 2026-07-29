/**
 * HomeDashboard service — H01 ~ H11
 * 对接 HomeDashboardController (/api/v1/homedashboard)
 */
import { get, put } from './api'

const BASE = '/homedashboard'

export const homeDashboardService = {
  /** H01 — GET /api/v1/homedashboard/kpi */
  getKpi() { return get(`${BASE}/kpi`) },

  /** H02 — GET /api/v1/homedashboard/chain-dist */
  getChainDist() { return get(`${BASE}/chain-dist`) },

  /** H03 — GET /api/v1/homedashboard/alerts */
  getAlerts() { return get(`${BASE}/alerts`) },

  /** H04 — PUT /api/v1/homedashboard/alerts/{id}/dismiss */
  dismissAlert(id) { return put(`${BASE}/alerts/${id}/dismiss`) },

  /** H05 — GET /api/v1/homedashboard/park-stats */
  getParkStats() { return get(`${BASE}/park-stats`) },

  /** H06 — GET /api/v1/homedashboard/patent-detail */
  getPatentDetail() { return get(`${BASE}/patent-detail`) },

  /** H07 — GET /api/v1/homedashboard/enterprise-detail */
  getEnterpriseDetail() { return get(`${BASE}/enterprise-detail`) },

  /** H08 — GET /api/v1/homedashboard/diagnosis-summary */
  getDiagnosisSummary() { return get(`${BASE}/diagnosis-summary`) },

  /** H09 — GET /api/v1/homedashboard/alert-detail/{id} */
  getAlertDetail(id) { return get(`${BASE}/alert-detail/${id}`) },

  /** H10 — GET /api/v1/homedashboard/park-patent-detail?parkName= */
  getParkPatentDetail(parkName) {
    return get(`${BASE}/park-patent-detail?parkName=${encodeURIComponent(parkName)}`)
  },

  /** H11 — GET /api/v1/homedashboard/park-highvalue-patents?parkName= */
  getParkHighValuePatents(parkName) {
    return get(`${BASE}/park-highvalue-patents?parkName=${encodeURIComponent(parkName)}`)
  }
}
