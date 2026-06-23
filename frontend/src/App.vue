<template>
  <div class="app-container">
    <router-view />
  </div>
</template>

<script setup>
import { useAppStore } from './stores/app'
const store = useAppStore()
</script>

<style>
:root {
  --bg-primary: #0d0d1a;
  --bg-secondary: #141428;
  --bg-card: #1a1a30;
  --bg-card-hover: #1f1f38;
  --bg-elevated: #1e1e35;
  --border-color: rgba(108, 92, 231, 0.15);
  --border-active: #6c5ce7;
  --text-primary: #e8e8f0;
  --text-secondary: #9898b0;
  --text-muted: #68687d;
  --accent: #6c5ce7;
  --accent-light: #8b7cf7;
  --accent-glow: rgba(108, 92, 231, 0.15);
  --green: #4ade80;
  --green-bg: rgba(74, 222, 128, 0.1);
  --red: #f87171;
  --red-bg: rgba(248, 113, 113, 0.1);
  --yellow: #facc15;
  --yellow-bg: rgba(250, 204, 21, 0.1);
  --orange: #fb923c;
  --radius-sm: 8px;
  --radius-md: 12px;
  --radius-lg: 16px;
  --radius-xl: 20px;
  --shadow-card: 0 2px 12px rgba(0,0,0,0.2);
  --shadow-elevated: 0 8px 32px rgba(0,0,0,0.3);
  --transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}
* { margin: 0; padding: 0; box-sizing: border-box; }
body {
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  background: var(--bg-primary);
  color: var(--text-primary);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  line-height: 1.5;
}
#app { height: 100vh; display: flex; flex-direction: column; }
.app-container { height: 100vh; display: flex; flex-direction: column; }
input, textarea, select, button { font-family: 'Inter', sans-serif; }
code, pre { font-family: 'JetBrains Mono', monospace; }
</style>
