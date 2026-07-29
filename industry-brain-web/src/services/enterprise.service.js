/**
 * 企业详情 API 服务 — E01~E05
 * 对接 EnterpriseController
 */
import { get } from './api'

const BASE = '/enterprise'

/** E01: 企业基本信息 */
export function getEnterprise(id) { return get(`${BASE}/${id}`) }

/** E02: 企业技术画像 */
export function getEnterpriseProfile(id) { return get(`${BASE}/${id}/profile`) }

/** E03: 企业专利列表 */
export function getEnterprisePatents(id, { page = 1, size = 10, keyword, type } = {}) {
  const params = new URLSearchParams({ page, size })
  if (keyword) params.set('keyword', keyword)
  if (type) params.set('type', type)
  return get(`${BASE}/${id}/patents?${params}`)
}

/** E04: 企业综合评估 */
export function getEnterpriseEvaluate(id) { return get(`${BASE}/${id}/evaluate`) }

/** E05: 企业模糊搜索 */
export function searchEnterprise(keyword) { return get(`${BASE}/search?keyword=${encodeURIComponent(keyword)}`) }

/** 本地企业列表 */
export function getLocalEnterprises(keyword) {
  const params = keyword ? `?keyword=${encodeURIComponent(keyword)}` : ''
  return get(`${BASE}/local/list${params}`)
}
