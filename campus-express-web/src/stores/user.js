import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

function readStoredUser() {
  try {
    return JSON.parse(localStorage.getItem('user') || 'null')
  } catch {
    localStorage.removeItem('user')
    return null
  }
}

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(readStoredUser())
  const role = computed(() => Number(user.value?.role ?? -1))

  function setAuth(nextToken, nextUser) {
    token.value = nextToken
    localStorage.setItem('token', nextToken)
    setUser(nextUser)
  }

  function setUser(nextUser) {
    user.value = nextUser
    localStorage.setItem('user', JSON.stringify(nextUser))
  }

  function clearAuth() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  return {
    token,
    user,
    role,
    setAuth,
    setUser,
    clearAuth
  }
})
