import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { apiLogout } from '../auth'

const API_BASE = 'http://localhost:8080/api'

export const useAppStore = defineStore('app', () => {
  const token = ref(localStorage.getItem('token') || '')
  const username = ref(localStorage.getItem('username') || '')

  const isLoggedIn = computed(() => !!token.value)

  function setAuth(t, u) {
    token.value = t
    username.value = u
    localStorage.setItem('token', t)
    localStorage.setItem('username', u)
  }

  async function logout() {
    try {
      await apiLogout()
    } catch (e) { /* ignore network errors, still clear local state */ }
    token.value = ''
    username.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('username')
  }

  async function apiGet(path) {
    const res = await fetch(API_BASE + path, {
      headers: { Authorization: 'Bearer ' + token.value }
    })
    if (!res.ok) throw new Error((await res.json()).error || 'Request failed')
    return res.json()
  }

  async function apiPost(path, body) {
    const res = await fetch(API_BASE + path, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: 'Bearer ' + token.value,
      },
      body: JSON.stringify(body),
    })
    if (!res.ok) throw new Error((await res.json()).error || 'Request failed')
    return res.json()
  }

  async function apiPut(path, body) {
    const res = await fetch(API_BASE + path, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        Authorization: 'Bearer ' + token.value,
      },
      body: JSON.stringify(body),
    })
    if (!res.ok) throw new Error((await res.json()).error || 'Request failed')
    return res.json()
  }

  async function apiDelete(path) {
    const res = await fetch(API_BASE + path, {
      method: 'DELETE',
      headers: { Authorization: 'Bearer ' + token.value },
    })
    if (!res.ok) throw new Error((await res.json()).error || 'Request failed')
    return res.json()
  }

  return { token, username, isLoggedIn, setAuth, logout, apiGet, apiPost, apiPut, apiDelete }
})
