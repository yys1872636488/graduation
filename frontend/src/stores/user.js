import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, getUserInfo } from '@/api/auth'
import router from '@/router'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(null)
  const roles = ref([])
  const permissions = ref([])
  const menus = ref([])

  async function login(loginForm) {
    const res = await loginApi(loginForm)
    token.value = res.data.token
    localStorage.setItem('token', res.data.token)
    await fetchUserInfo()
  }

  async function fetchUserInfo() {
    const res = await getUserInfo()
    userInfo.value = res.data.user
    roles.value = res.data.roles || []
    permissions.value = res.data.permissions || []
    menus.value = res.data.menus || []
  }

  // 页面刷新后 Pinia 状态会清空，路由守卫通过此方法恢复当前用户信息
  async function ensureUserInfo() {
    if (!userInfo.value) {
      await fetchUserInfo()
    }
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    roles.value = []
    permissions.value = []
    menus.value = []
    localStorage.removeItem('token')
    router.push('/login')
  }

  function hasPermission(perm) {
    return permissions.value.includes(perm)
  }

  return { token, userInfo, roles, permissions, menus, login, fetchUserInfo, ensureUserInfo, logout, hasPermission }
})