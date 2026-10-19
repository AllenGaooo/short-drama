import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userId: localStorage.getItem('userId') || '',
    phone: localStorage.getItem('phone') || '',
    userType: localStorage.getItem('userType') || '',
    userInfo: null
  }),
  actions: {
    setLogin(data) {
      // 后端返回 { userId, token, phone, userType }
      this.token = data.token || ''
      this.userId = data.userId || ''
      this.phone = data.phone || ''
      this.userType = data.userType || 'user'
      localStorage.setItem('token', this.token)
      localStorage.setItem('userId', this.userId)
      localStorage.setItem('phone', this.phone)
      localStorage.setItem('userType', this.userType)
    },
    setUserInfo(info) {
      this.userInfo = info
    },
    logout() {
      this.token = ''
      this.userId = ''
      this.phone = ''
      this.userType = ''
      this.userInfo = null
      localStorage.removeItem('token')
      localStorage.removeItem('userId')
      localStorage.removeItem('phone')
      localStorage.removeItem('userType')
    },
    get isLogin() {
      return !!this.token
    }
  }
})
