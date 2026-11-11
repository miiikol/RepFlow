import { defineStore } from 'pinia'
import { api } from '../api'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('rf_token') || '',
    user: JSON.parse(localStorage.getItem('rf_user') || 'null'),
    meta: null
  }),
  getters: {
    permissions: (state) => state.user?.permissions || [],
    roles: (state) => state.user?.roles || [],
    isAdmin: (state) => (state.user?.roles || []).includes('ADMIN')
  },
  actions: {
    async login(form) {
      const data = await api.login(form)
      this.token = data.token
      this.user = data.user
      localStorage.setItem('rf_token', data.token)
      localStorage.setItem('rf_user', JSON.stringify(data.user))
      await this.loadMeta()
      return data
    },
    async loadMe() {
      this.user = await api.me()
      localStorage.setItem('rf_user', JSON.stringify(this.user))
    },
    async loadMeta() {
      this.meta = await api.meta()
      return this.meta
    },
    logout() {
      this.token = ''
      this.user = null
      this.meta = null
      localStorage.removeItem('rf_token')
      localStorage.removeItem('rf_user')
    },
    hasPerm(code) {
      if (!code) return true
      if (this.isAdmin) return true
      return this.permissions.includes(code)
    },
    hasAnyPerm(codes) {
      if (!codes || !codes.length) return true
      return codes.some((code) => this.hasPerm(code))
    }
  }
})
