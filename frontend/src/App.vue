<template>
  <div class="app-container">
    <router-view v-slot="{ Component }">
      <transition name="page" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </div>
</template>

<script setup>
import { useAppStore } from './stores/app'
const store = useAppStore()
</script>

<style>
:root {
  /* Warm office palette */
  --bg-deep: #f0ece4;
  --bg-primary: #f5f2ec;
  --bg-secondary: #ebe6dc;
  --bg-card: #faf7f2;
  --bg-card-hover: #fffcf7;
  --bg-elevated: #ffffff;
  --bg-glass: rgba(250, 247, 242, 0.72);
  --bg-glass-strong: rgba(255, 255, 255, 0.88);

  --border-color: rgba(0, 0, 0, 0.06);
  --border-light: rgba(0, 0, 0, 0.10);
  --border-active: rgba(108, 92, 231, 0.30);

  --text-primary: #2a2620;
  --text-secondary: #6a6558;
  --text-muted: #9a9588;
  --text-dim: #c0baae;

  --accent: #6c5ce7;
  --accent-light: #8b7cf7;
  --accent-dim: rgba(108, 92, 231, 0.10);
  --accent-glow: rgba(108, 92, 231, 0.06);

  --green: #22c55e;
  --green-bg: rgba(34, 197, 94, 0.08);
  --red: #ef4444;
  --red-bg: rgba(239, 68, 68, 0.08);
  --yellow: #eab308;
  --yellow-bg: rgba(234, 179, 8, 0.08);
  --orange: #f97316;
  --orange-bg: rgba(249, 115, 22, 0.08);
  --blue: #3b82f6;
  --blue-bg: rgba(59, 130, 246, 0.08);
  --pink: #ec4899;
  --pink-bg: rgba(236, 72, 153, 0.08);

  --radius-xs: 4px;
  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-lg: 14px;
  --radius-xl: 18px;
  --radius-2xl: 24px;

  --shadow-sm: 0 1px 3px rgba(0,0,0,0.06);
  --shadow-md: 0 4px 12px rgba(0,0,0,0.05);
  --shadow-lg: 0 8px 28px rgba(0,0,0,0.06);
  --shadow-xl: 0 16px 48px rgba(0,0,0,0.08);
  --shadow-glow: 0 4px 20px rgba(108,92,231,0.10);

  --transition-fast: all 0.15s cubic-bezier(0.4, 0, 0.2, 1);
  --transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  --transition-slow: all 0.35s cubic-bezier(0.16, 1, 0.3, 1);
}

* { margin: 0; padding: 0; box-sizing: border-box; }
html { color-scheme: light; }
body {
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  background: var(--bg-deep);
  color: var(--text-primary);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  line-height: 1.5;
}
#app { height: 100vh; display: flex; flex-direction: column; }
.app-container { height: 100vh; display: flex; flex-direction: column; }
input, textarea, select, button { font-family: 'Inter', sans-serif; }
code, pre { font-family: 'JetBrains Mono', monospace; font-size: 0.9em; }

/* Page transitions */
.page-enter-active { animation: pageIn 0.25s cubic-bezier(0.16, 1, 0.3, 1) forwards; }
.page-leave-active { animation: pageOut 0.15s cubic-bezier(0.4, 0, 1, 1) forwards; }
@keyframes pageIn { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }
@keyframes pageOut { from { opacity: 1; transform: translateY(0); } to { opacity: 0; transform: translateY(-6px); } }

/* Glass utility */
.glass {
  background: var(--bg-glass);
  backdrop-filter: blur(24px) saturate(1.3);
  -webkit-backdrop-filter: blur(24px) saturate(1.3);
  border: 1px solid var(--border-color);
}
.glass-strong {
  background: var(--bg-glass-strong);
  backdrop-filter: blur(28px) saturate(1.4);
  -webkit-backdrop-filter: blur(28px) saturate(1.4);
  border: 1px solid var(--border-light);
}
.card-hover { transition: var(--transition-slow); }
.card-hover:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 32px rgba(108,92,231,0.06);
  border-color: var(--border-active);
}

/* Focus ring */
*:focus-visible {
  outline: none;
  box-shadow: 0 0 0 2px var(--accent), 0 0 0 4px var(--accent-glow);
  border-radius: var(--radius-sm);
}
</style>
