/**
 * Auth service — A01 ~ A03
 * 对接 AuthController (/api/v1/auth)
 */
import { get, post } from './api'

const BASE = '/auth'

export const authService = {
  /** A01 — POST /api/v1/auth/login */
  async login(username, password) {
    return post(`${BASE}/login`, { username, password })
  },

  /** A02 — POST /api/v1/auth/logout */
  async logout() {
    return post(`${BASE}/logout`)
  },

  /** A03 — GET /api/v1/auth/info */
  async getCurrentUser() {
    return get(`${BASE}/info`)
  }
}
