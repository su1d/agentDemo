<template>
  <div class="login-page">
    <div class="login-bg"></div>
    <div class="login-container">
      <div class="login-card">
        <div class="login-brand">
          <div class="brand-icon">
            <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
              <rect width="48" height="48" rx="14" fill="#6c5ce7" fill-opacity="0.15"/>
              <path d="M16 24C16 19.58 19.58 16 24 16C28.42 16 32 19.58 32 24" stroke="#6c5ce7" stroke-width="2.5" stroke-linecap="round"/>
              <path d="M24 20V24" stroke="#6c5ce7" stroke-width="2.5" stroke-linecap="round"/>
              <circle cx="24" cy="28" r="2" fill="#6c5ce7"/>
              <path d="M14 32C14 27.58 17.58 24 22 24H26C30.42 24 34 27.58 34 32" stroke="#8b7cf7" stroke-width="2" stroke-linecap="round" stroke-dasharray="3 3"/>
            </svg>
          </div>
          <h1 class="brand-title">Multi-Agent System</h1>
          <p class="brand-subtitle">智能协作平台</p>
        </div>

        <div class="login-tabs">
          <button :class="['tab-btn', tab === 'login' ? 'active' : '']" @click="tab = 'login'">登录</button>
          <button :class="['tab-btn', tab === 'register' ? 'active' : '']" @click="tab = 'register'">注册</button>
        </div>

        <form @submit.prevent="handleSubmit" class="login-form">
          <div class="form-group">
            <label>用户名</label>
            <div class="input-wrapper">
              <svg class="input-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="8" r="4"/><path d="M4 22c0-4.42 3.58-8 8-8s8 3.58 8 8"/></svg>
              <input v-model="form.username" type="text" placeholder="请输入用户名" required />
            </div>
          </div>
          <div class="form-group">
            <label>密码</label>
            <div class="input-wrapper">
              <svg class="input-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
              <input v-model="form.password" type="password" placeholder="请输入密码" required />
            </div>
          </div>

          <transition name="fade">
            <div v-if="error" class="error-msg">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 8v4M12 16h.01"/></svg>
              {{ error }}
            </div>
          </transition>

          <button type="submit" class="submit-btn" :disabled="loading">
            <span v-if="loading" class="spinner"></span>
            <span v-else>{{ tab === 'login' ? '登录' : '注册' }}</span>
          </button>
        </form>
      </div>
    </div>
  </div>
</template>

<script>
import { login, register } from '../auth'

export default {
  name: 'LoginView',
  data() {
    return {
      tab: 'login',
      form: { username: '', password: '' },
      loading: false,
      error: '',
    }
  },
  methods: {
    async handleSubmit() {
      this.error = ''
      this.loading = true
      try {
        const fn = this.tab === 'login' ? login : register
        const res = await fn(this.form.username, this.form.password)
        localStorage.setItem('token', res.token)
        localStorage.setItem('username', res.username)
        this.$router.push('/')
      } catch (e) {
        this.error = e.message || '操作失败，请重试'
      } finally {
        this.loading = false
      }
    },
  },
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-primary);
  position: relative;
  overflow: hidden;
}

.login-bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse at 20% 50%, rgba(108,92,231,0.08) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 20%, rgba(74,222,128,0.04) 0%, transparent 50%),
    radial-gradient(ellipse at 50% 80%, rgba(251,146,60,0.04) 0%, transparent 50%);
  pointer-events: none;
}

.login-container {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 420px;
  padding: 20px;
}

.login-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-xl);
  padding: 40px 32px;
  backdrop-filter: blur(20px);
  box-shadow: var(--shadow-elevated);
}

.login-brand {
  text-align: center;
  margin-bottom: 32px;
}

.brand-icon {
  display: inline-flex;
  margin-bottom: 16px;
}

.brand-title {
  font-size: 24px;
  font-weight: 800;
  color: var(--text-primary);
  margin-bottom: 4px;
  letter-spacing: -0.5px;
}

.brand-subtitle {
  font-size: 14px;
  color: var(--text-muted);
  font-weight: 400;
}

.login-tabs {
  display: flex;
  gap: 6px;
  margin-bottom: 24px;
  background: var(--bg-secondary);
  padding: 4px;
  border-radius: var(--radius-sm);
}

.tab-btn {
  flex: 1;
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition);
}

.tab-btn.active {
  background: var(--accent);
  color: #fff;
  box-shadow: 0 2px 8px rgba(108,92,231,0.3);
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-group label {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 12px;
  color: var(--text-muted);
  pointer-events: none;
}

.form-group input {
  width: 100%;
  padding: 10px 12px 10px 36px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--bg-secondary);
  color: var(--text-primary);
  font-size: 14px;
  outline: none;
  transition: var(--transition);
}

.form-group input:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-glow);
}

.form-group input::placeholder {
  color: var(--text-muted);
}

.error-msg {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 12px;
  background: var(--red-bg);
  border: 1px solid rgba(248,113,113,0.2);
  border-radius: var(--radius-sm);
  color: var(--red);
  font-size: 13px;
}

.submit-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  border: none;
  border-radius: var(--radius-sm);
  background: linear-gradient(135deg, var(--accent), var(--accent-light));
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: var(--transition);
  margin-top: 4px;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(108,92,231,0.3);
}

.submit-btn:active:not(:disabled) {
  transform: translateY(0);
}

.submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style>
