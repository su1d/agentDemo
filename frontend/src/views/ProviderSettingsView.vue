<template>
  <div class="providers-page">
    <div class="page-header">
      <h2>
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="3"/>
          <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/>
        </svg>
        LLM 提供商
      </h2>
    </div>

    <div class="providers-grid">
      <div
        v-for="provider in providers"
        :key="provider.key"
        class="provider-card"
        :class="{ 'provider-active': provider.enabled }"
      >
        <div class="provider-header">
          <div class="provider-name">
            <span class="provider-badge" :class="provider.enabled ? 'badge-on' : 'badge-off'">
              {{ provider.enabled ? '启用' : '停用' }}
            </span>
            <h3>{{ provider.name }}</h3>
          </div>
          <div class="provider-key">{{ provider.key }}</div>
        </div>

        <div class="provider-body">
          <div class="provider-field">
            <label>API Base URL</label>
            <input
              v-model="provider.baseUrl"
              class="provider-input"
              placeholder="https://api.openai.com/v1"
              :disabled="!editing[provider.key]"
            />
          </div>

          <div class="provider-field">
            <label>API Key</label>
            <div class="input-with-toggle">
              <input
                :type="showKeys[provider.key] ? 'text' : 'password'"
                v-model="provider.apiKey"
                class="provider-input"
                placeholder="sk-..."
                :disabled="!editing[provider.key]"
              />
              <button class="toggle-key" @click="toggleKey(provider.key)">
                <svg v-if="showKeys[provider.key]" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19M14.12 14.12a3 3 0 1 1-4.24-4.24"/>
                  <line x1="1" y1="1" x2="23" y2="23"/>
                </svg>
                <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                  <circle cx="12" cy="12" r="3"/>
                </svg>
              </button>
            </div>
          </div>

          <div class="provider-field">
            <label>默认模型</label>
            <select
              v-model="provider.defaultModel"
              class="provider-select"
              :disabled="!editing[provider.key]"
            >
              <option v-for="m in provider.models" :key="m" :value="m">{{ m }}</option>
            </select>
          </div>
        </div>

        <div class="provider-footer">
          <label class="toggle-switch">
            <input
              type="checkbox"
              v-model="provider.enabled"
              :disabled="editing[provider.key]"
            />
            <span class="toggle-slider"></span>
          </label>
          <div class="provider-actions">
            <template v-if="editing[provider.key]">
              <button class="btn-save" @click="saveProvider(provider.key)">保存</button>
              <button class="btn-cancel" @click="cancelEdit(provider.key)">取消</button>
            </template>
            <button v-else class="btn-edit" @click="startEdit(provider.key)">编辑</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
const API_BASE = 'http://localhost:8080/api'

export default {
  name: 'ProviderSettingsView',
  emits: ['notify'],
  data() {
    return {
      providers: [],
      editing: {},
      originals: {},
      showKeys: {},
    }
  },
  mounted() {
    this.fetchProviders()
  },
  methods: {
    async fetchProviders() {
      try {
        const token = localStorage.getItem('token')
        const res = await fetch(`${API_BASE}/providers`, {
          headers: { Authorization: 'Bearer ' + token }
        })
        this.providers = await res.json()
      } catch (e) {
        console.error('Failed to load providers:', e)
      }
    },
    startEdit(key) {
      this.editing[key] = true
      const provider = this.providers.find(p => p.key === key)
      this.originals[key] = JSON.parse(JSON.stringify(provider))
    },
    cancelEdit(key) {
      this.editing[key] = false
      const idx = this.providers.findIndex(p => p.key === key)
      if (idx >= 0) {
        this.providers[idx] = this.originals[key]
      }
    },
    async saveProvider(key) {
      const provider = this.providers.find(p => p.key === key)
      try {
        const token = localStorage.getItem('token')
        const res = await fetch(`${API_BASE}/providers/${key}`, {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
            Authorization: 'Bearer ' + token,
          },
          body: JSON.stringify({
            apiKey: provider.apiKey,
            baseUrl: provider.baseUrl,
            defaultModel: provider.defaultModel,
            enabled: provider.enabled,
          }),
        })
        const result = await res.json()
        if (result.error) {
          this.$emit('notify', result.error, 'warn')
          return
        }
        this.editing[key] = false
        this.$emit('notify', provider.name + ' 配置已更新', 'ok')
      } catch (e) {
        this.$emit('notify', '保存失败: ' + e.message, 'warn')
      }
    },
    toggleKey(key) {
      this.showKeys[key] = !this.showKeys[key]
    },
  },
}
</script>

<style scoped>
.providers-page {
  padding: 24px;
  height: 100%;
  overflow-y: auto;
}

.page-header h2 {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 24px;
}

.providers-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(420px, 1fr));
  gap: 20px;
}

.provider-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  overflow: hidden;
  transition: var(--transition);
}

.provider-card:hover {
  border-color: var(--border-active);
  box-shadow: 0 4px 20px var(--accent-glow);
}

.provider-card.provider-active {
  border-color: rgba(74, 222, 128, 0.2);
}

.provider-header {
  padding: 16px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--border-color);
}

.provider-name {
  display: flex;
  align-items: center;
  gap: 10px;
}

.provider-name h3 {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.provider-badge {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.badge-on {
  background: var(--green-bg);
  color: var(--green);
}

.badge-off {
  background: rgba(248, 113, 113, 0.1);
  color: #f87171;
}

.provider-key {
  font-size: 12px;
  color: var(--text-muted);
  font-family: monospace;
  padding: 3px 8px;
  background: var(--bg-secondary);
  border-radius: 4px;
}

.provider-body {
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.provider-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.provider-field label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.provider-input,
.provider-select {
  padding: 9px 12px;
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-primary);
  font-size: 13px;
  font-family: inherit;
  transition: var(--transition);
  width: 100%;
  box-sizing: border-box;
}

.provider-input:focus,
.provider-select:focus {
  outline: none;
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-glow);
}

.provider-input:disabled,
.provider-select:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.input-with-toggle {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

.input-with-toggle .provider-input {
  flex: 1;
}

.toggle-key {
  padding: 8px;
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  cursor: pointer;
  color: var(--text-muted);
  display: flex;
  align-items: center;
  transition: var(--transition);
  flex-shrink: 0;
}

.toggle-key:hover {
  color: var(--text-primary);
  border-color: var(--border-active);
}

.provider-footer {
  padding: 12px 20px;
  border-top: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toggle-switch {
  position: relative;
  display: inline-block;
  width: 40px;
  height: 22px;
  cursor: pointer;
}

.toggle-switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.toggle-slider {
  position: absolute;
  inset: 0;
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 11px;
  transition: var(--transition);
}

.toggle-switch input:checked + .toggle-slider {
  background: var(--green);
  border-color: var(--green);
}

.toggle-slider::before {
  content: '';
  position: absolute;
  width: 16px;
  height: 16px;
  left: 2px;
  top: 2px;
  background: #fff;
  border-radius: 50%;
  transition: var(--transition);
}

.toggle-switch input:checked + .toggle-slider::before {
  transform: translateX(18px);
}

.provider-actions {
  display: flex;
  gap: 8px;
}

.btn-edit,
.btn-save,
.btn-cancel {
  padding: 7px 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: var(--transition);
  border: 1px solid var(--border-color);
}

.btn-edit {
  background: transparent;
  color: var(--accent);
}

.btn-edit:hover {
  background: var(--accent-glow);
}

.btn-save {
  background: var(--accent);
  color: #fff;
  border-color: var(--accent);
}

.btn-save:hover {
  background: var(--accent-light);
}

.btn-cancel {
  background: transparent;
  color: var(--text-secondary);
}

.btn-cancel:hover {
  background: var(--bg-secondary);
  color: var(--text-primary);
}
</style>
