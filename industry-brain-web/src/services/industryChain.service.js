/**
 * 产业链全景 API 服务 — C01~C09
 * 对接 IndustryChainController
 */
import { get, post, put, del } from './api'

const BASE = '/industry/chain'

/** C01: 产业链目录列表 */
export function getChainList() { return get(`${BASE}/list`) }

/** C02: 产业链全部节点 (含诊断标签) */
export function getChainNodes(chainId) { return get(`${BASE}/${chainId}/nodes`) }

/** C03: 节点关联关系 */
export function getChainLinks(chainId) { return get(`${BASE}/${chainId}/links`) }

/** C04: 节点详情 (含关联企业) */
export function getNodeDetail(nodeId) { return get(`${BASE}/node/${nodeId}`) }

/** C05: 创建节点 */
export function createNode(data) { return post(`${BASE}/node`, data) }

/** C06: 更新节点 */
export function updateNode(id, data) { return put(`${BASE}/node/${id}`, data) }

/** C07: 删除节点 */
export function deleteNode(id) { return del(`${BASE}/node/${id}`) }

/** C08: 创建节点关联 */
export function createLink(data) { return post(`${BASE}/link`, data) }

/** C09: 删除节点关联 */
export function deleteLink(id) { return del(`${BASE}/link/${id}`) }
