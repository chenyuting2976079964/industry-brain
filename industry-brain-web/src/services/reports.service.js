/**
 * Reports service — R01 ~ R08
 * 对接 RecommendationController (/api/v1/recommendation)
 */
import { get, post, del } from './api'

const BASE = '/recommendation'

export const reportsService = {
  /** R01 — GET /api/v1/recommendation/reports?page=&size= */
  getReports(page = 1, size = 10) {
    return get(`${BASE}/reports?page=${page}&size=${size}`)
  },

  /** R02 — GET /api/v1/recommendation/reports/{id} */
  getReportDetail(id) { return get(`${BASE}/reports/${id}`) },

  /** R03 — POST /api/v1/recommendation/reports/generate
   *  后端: @RequestBody ReportGenerateRequest { type, scope, count, format, chainId, nodeId, enterpriseId } */
  generateReport(params) { return post(`${BASE}/reports/generate`, params) },

  /** R04 — GET /api/v1/recommendation/reports/{id}/download
   *  后端返回: fileUrl string */
  downloadReport(id) { return get(`${BASE}/reports/${id}/download`) },

  /** R05 — DELETE /api/v1/recommendation/reports/{id} */
  deleteReport(id) { return del(`${BASE}/reports/${id}`) },

  /** R06 — GET /api/v1/recommendation/reports/templates */
  getTemplates() { return get(`${BASE}/reports/templates`) },

  /** R07 — GET /api/v1/recommendation/reports/stats
   *  后端返回: ReportStatsResponse */
  getStats() { return get(`${BASE}/reports/stats`) },

  /** R08 — POST /api/v1/recommendation/reports/export-all
   *  后端: @RequestBody ExportAllRequest { format } */
  exportAll(format = 'excel') { return post(`${BASE}/reports/export-all`, { format }) }
}
