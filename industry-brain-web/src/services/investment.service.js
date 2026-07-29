/**
 * Investment service — I01 ~ I07
 * 对接 RecommendationController (/api/v1/recommendation)
 */
import { get, post, del } from './api'

const BASE = '/recommendation'

export const investmentService = {
  /** I01 — GET /api/v1/recommendation/match-list
   *  后端: @ModelAttribute InvestmentMatchRequest
   *  字段: weakness, matchMin, scale, sortBy, keyword, nodeId, page, size, tab */
  async getEnterprises(params = {}) {
    const qs = new URLSearchParams()
    if (params.weakness && params.weakness !== 'all') qs.set('weakness', params.weakness)
    if (params.minMatch && params.minMatch !== 'all') qs.set('matchMin', params.minMatch)
    if (params.scale && params.scale !== 'all') qs.set('scale', params.scale)
    if (params.sortBy) qs.set('sortBy', params.sortBy)
    if (params.keyword) qs.set('keyword', params.keyword)
    if (params.nodeId) qs.set('nodeId', params.nodeId)
    if (params.tab) qs.set('tab', params.tab)
    if (params.page) qs.set('page', params.page)
    if (params.size) qs.set('size', params.size)
    const query = qs.toString()
    return get(`${BASE}/match-list${query ? '?' + query : ''}`)
  },

  /** I02 — GET /api/v1/recommendation/enterprise/{id}?nodeId=
   *  后端返回: EnterpriseInvestmentDetailResponse */
  getEnterpriseDetail(id, nodeId) {
    const qs = nodeId ? `?nodeId=${nodeId}` : ''
    return get(`${BASE}/enterprise/${id}${qs}`)
  },

  /** I03 — GET /api/v1/recommendation/wishlist
   *  后端返回: WishlistResponse { items, total } */
  getWishlist() { return get(`${BASE}/wishlist`) },

  /** I04 — POST /api/v1/recommendation/wishlist
   *  后端: @RequestBody WishlistAddRequest { enterpriseId, nodeId, recommendationId } */
  addToWishlist(enterpriseId, nodeId, recommendationId) {
    return post(`${BASE}/wishlist`, { enterpriseId, nodeId, recommendationId })
  },

  /** I05 — DELETE /api/v1/recommendation/wishlist/{id} */
  removeFromWishlist(id) { return del(`${BASE}/wishlist/${id}`) },

  /** I06 — POST /api/v1/recommendation/rematch?nodeId=
   *  后端返回: taskId string */
  rematch(nodeId) {
    const qs = nodeId ? `?nodeId=${nodeId}` : ''
    return post(`${BASE}/rematch${qs}`)
  },

  /** I07 — POST /api/v1/recommendation/export
   *  后端: @RequestBody RecommendationRequest { chainId, targetLinks, regionCode, minThreshold, limit }
   *  前端按需组装导出参数 */
  exportList(params = {}) {
    return post(`${BASE}/export`, params)
  }
}
