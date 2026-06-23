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
  /* Deep space - refined palette */
  --bg-deep: #06060f;
  --bg-primary: #0a0a16;
  --bg-secondary: #10101e;
  --bg-card: #14142a;
  --bg-card-hover: #181832;
  --bg-elevated: #1c1c38;
  --bg-glass: rgba(20, 20, 42, 0.55);
  --bg-glass-strong: rgba(26, 26, 52, 0.75);

  --border-color: rgba(108, 92, 231, 0.10);
  --border-light: rgba(108, 92, 231, 0.18);
  --border-active: rgba(108, 92, 231, 0.40);

  --text-primary: #e6e6f2;
  --text-secondary: #9898b8;
  --text-muted: #68688a;
  --text-dim: #48486a;

  --accent: #6c5ce7;
  --accent-light: #8b7cf7;
  --accent-dim: rgba(108, 92, 231, 0.12);
  --accent-glow: rgba(108, 92, 231, 0.08);

  --green: #4ade80;
  --green-bg: rgba(74, 222, 128, 0.08);
  --red: #f87171;
  --red-bg: rgba(248, 113, 113, 0.08);
  --yellow: #facc15;
  --yellow-bg: rgba(250, 204, 21, 0.08);
  --orange: #fb923c;
  --orange-bg: rgba(251, 146, 60, 0.08);
  --blue: #60a5fa;
  --blue-bg: rgba(96, 165, 250, 0.08);
  --pink: #f472b6;
  --pink-bg: rgba(244, 114, 182, 0.08);

  --radius-xs: 4px;
  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-lg: 14px;
  --radius-xl: 18px;
  --radius-2xl: 24px;

  --shadow-sm: 0 1px 4px rgba(0,0,0,0.3);
  --shadow-md: 0 4px 16px rgba(0,0,0,0.25);
  --shadow-lg: 0 8px 32px rgba(0,0,0,0.3);
  --shadow-xl: 0 16px 48px rgba(0,0,0,0.4);
  --shadow-glow: 0 4px 24px rgba(108,92,231,0.12);

  --transition-fast: all 0.15s cubic-bezier(0.4, 0, 0.2, 1);
  --transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  --transition-slow: all 0.35s cubic-bezier(0.16, 1, 0.3, 1);
}

* { margin: 0; padding: 0; box-sizing: border-box; }
html { color-scheme: dark; }
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
  box-shadow: 0 8px 32px rgba(108,92,231,0.08);
  border-color: var(--border-active);
}

/* Focus ring */
*:focus-visible {
  outline: none;
  box-shadow: 0 0 0 2px var(--accent), 0 0 0 4px var(--accent-glow);
  border-radius: var(--radius-sm);
}
</style>
