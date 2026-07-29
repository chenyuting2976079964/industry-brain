/**
 * API 请求层 — 使用 fetch 对接 Spring Boot 后端
 * 后端 base: /api/v1
 * 响应格式: { code: 200, msg: "操作成功", data: ... }
 */

const BASE_URL = '/api/v1'

function getToken() {
  return sessionStorage.getItem('token') || ''
}

async function request(method, path, body) {
  const url = `${BASE_URL}${path}`
  const options = {
    method,
    headers: { 'Content-Type': 'application/json' },
  }

  // Attach Authorization header if token exists
  const token = getToken()
  if (token) {
    options.headers['Authorization'] = `Bearer ${token}`
  }

  if (body) options.body = JSON.stringify(body)

  console.log(`[API] ${method} ${url}`)
  const res = await fetch(url, options)

  // Handle 401 unauthorized
  if (res.status === 401) {
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('loggedIn')
    window.location.hash = '#/login'
    throw new Error('登录已过期，请重新登录')
  }

  const json = await res.json()

  // AjaxResult: { code, msg, data }
  if (json.code !== 200) {
    throw new Error(json.msg || json.message || 'Request failed')
  }
  return json.data
}

export function get(path) { return request('GET', path) }
export function post(path, body) { return request('POST', path, body) }
export function put(path, body) { return request('PUT', path, body) }
export function del(path) { return request('DELETE', path) }
