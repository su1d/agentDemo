const BASE_URL = 'http://localhost:8080/api'

async function request(url, options = {}) {
  const token = localStorage.getItem('token')
  const headers = { 'Content-Type': 'application/json', ...options.headers }
  if (token) headers['Authorization'] = 'Bearer ' + token

  const res = await fetch(BASE_URL + url, { ...options, headers })
  const contentType = res.headers.get('content-type') || ''
  const isJson = contentType.includes('application/json')
  const data = isJson ? await res.json() : null
  if (!res.ok) {
    throw new Error(data?.error || data?.message || '请求失败 (' + res.status + ')')
  }
  return data
}

export async function login(username, password) {
  return request('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  })
}

export async function register(username, password) {
  return request('/auth/register', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  })
}

export async function apiLogout() {
  return request('/auth/logout', {
    method: 'POST',
  })
}

export async function apiChat(body) {
  return request('/chat', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export async function apiGet(url) {
  return request(url)
}
