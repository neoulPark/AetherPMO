<template>
  <aside class="sidebar">
    <div class="logo-area">
      <div class="logo-icon">
        <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
          <rect x="1" y="1" width="7" height="7" rx="1.5" fill="#6c5ce7"/>
          <rect x="12" y="1" width="7" height="7" rx="1.5" fill="#6c5ce7" opacity="0.6"/>
          <rect x="1" y="12" width="7" height="7" rx="1.5" fill="#6c5ce7" opacity="0.6"/>
          <rect x="12" y="12" width="7" height="7" rx="1.5" fill="#6c5ce7" opacity="0.4"/>
        </svg>
      </div>
      <div class="logo-text">
        <span class="logo-name">AetherPMO</span>
        <span class="logo-sub">사업관리 플랫폼</span>
      </div>
    </div>

    <nav class="nav">
      <div class="nav-section">
        <div class="nav-section-label">PROJECTS</div>

        <div class="nav-group">
          <div class="nav-item nav-parent" :class="{ active: isProjectsActive }" @click="toggleProjects">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/>
            </svg>
            <span>프로젝트 관리</span>
            <svg class="chevron" :class="{ rotated: projectsOpen }" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="6 9 12 15 18 9"/>
            </svg>
          </div>
          <div v-if="projectsOpen" class="nav-children">
            <router-link to="/projects/bidding" class="nav-item nav-child" :class="{ active: route.path.startsWith('/projects/bidding') }">
              입찰 단계
            </router-link>
            <router-link to="/projects/execution" class="nav-item nav-child" :class="{ active: route.path === '/projects/execution' || (route.path.startsWith('/projects/') && !route.path.includes('bidding') && !route.path.includes('completed') && route.path !== '/projects/execution') }">
              수행 단계
            </router-link>
            <router-link to="/projects/completed" class="nav-item nav-child" :class="{ active: route.path === '/projects/completed' }">
              종료 단계
            </router-link>
          </div>
        </div>

        <router-link to="/issues" class="nav-item" :class="{ active: route.path === '/issues' }">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <line x1="12" y1="8" x2="12" y2="12"/>
            <line x1="12" y1="16" x2="12.01" y2="16"/>
          </svg>
          <span>이슈 / 리스크</span>
        </router-link>

        <router-link to="/action-items" class="nav-item" :class="{ active: route.path === '/action-items' }">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="9 11 12 14 22 4"/>
            <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/>
          </svg>
          <span>액션 아이템</span>
        </router-link>
      </div>

      <div class="nav-section">
        <div class="nav-section-label">DOCUMENTS</div>

        <router-link to="/official-docs" class="nav-item" :class="{ active: route.path === '/official-docs' }">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
            <polyline points="14 2 14 8 20 8"/>
            <line x1="16" y1="13" x2="8" y2="13"/>
            <line x1="16" y1="17" x2="8" y2="17"/>
            <polyline points="10 9 9 9 8 9"/>
          </svg>
          <span>공문 관리</span>
        </router-link>

        <router-link to="/meetings" class="nav-item" :class="{ active: route.path === '/meetings' }">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
            <line x1="16" y1="2" x2="16" y2="6"/>
            <line x1="8" y1="2" x2="8" y2="6"/>
            <line x1="3" y1="10" x2="21" y2="10"/>
          </svg>
          <span>회의 관리</span>
        </router-link>

        <router-link to="/templates" class="nav-item" :class="{ active: route.path === '/templates' }">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M4 22h14a2 2 0 0 0 2-2V7.5L14.5 2H6a2 2 0 0 0-2 2v4"/>
            <polyline points="14 2 14 8 20 8"/>
            <path d="M2 15h10"/>
            <path d="M9 18l3-3-3-3"/>
          </svg>
          <span>산출물 템플릿</span>
        </router-link>
      </div>
    </nav>

    <div class="sidebar-bottom">
      <div class="theme-toggle-row">
        <span class="theme-label">다크 모드</span>
        <div class="toggle-switch" :class="{ on: isDark }" @click="isDark = !isDark">
          <div class="toggle-thumb"></div>
        </div>
      </div>
      <div class="user-info">
        <div class="user-avatar">안</div>
        <div class="user-details">
          <span class="user-name">안유경 PM</span>
          <span class="user-role">Project Manager</span>
        </div>
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const isDark = ref(true)
const projectsOpen = ref(true)

const isProjectsActive = computed(() =>
  route.path.startsWith('/projects')
)

function toggleProjects() {
  projectsOpen.value = !projectsOpen.value
}
</script>

<style scoped>
.sidebar {
  width: var(--sidebar-width);
  height: 100vh;
  background: var(--bg-surface);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  position: fixed;
  top: 0;
  left: 0;
  z-index: 100;
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 16px 12px;
  border-bottom: 1px solid var(--border);
}

.logo-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(108, 92, 231, 0.15);
  border-radius: 8px;
  flex-shrink: 0;
}

.logo-text {
  display: flex;
  flex-direction: column;
}

.logo-name {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 0.3px;
}

.logo-sub {
  font-size: 10px;
  color: var(--text-muted);
  margin-top: 1px;
}

.nav {
  flex: 1;
  overflow-y: auto;
  padding: 12px 0;
}

.nav-section {
  margin-bottom: 16px;
}

.nav-section-label {
  font-size: 10px;
  font-weight: 600;
  color: var(--text-muted);
  padding: 4px 16px 6px;
  letter-spacing: 0.8px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 16px;
  cursor: pointer;
  color: var(--text-secondary);
  font-size: 13.5px;
  border-radius: 6px;
  margin: 1px 8px;
  transition: all 0.15s;
  text-decoration: none;
}

.nav-item:hover {
  background: var(--bg-surface2);
  color: var(--text-primary);
}

.nav-item.active {
  background: var(--color-primary);
  color: white;
}

.nav-parent {
  justify-content: flex-start;
}

.nav-parent .chevron {
  margin-left: auto;
  transition: transform 0.2s;
}

.nav-parent .chevron.rotated {
  transform: rotate(180deg);
}

.nav-child {
  padding: 7px 16px 7px 38px;
  font-size: 13px;
}

.sidebar-bottom {
  border-top: 1px solid var(--border);
  padding: 12px;
}

.theme-toggle-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 4px;
  margin-bottom: 10px;
}

.theme-label {
  font-size: 12px;
  color: var(--text-secondary);
}

.toggle-switch {
  width: 36px;
  height: 20px;
  border-radius: 10px;
  background: var(--border);
  cursor: pointer;
  position: relative;
  transition: background 0.2s;
}

.toggle-switch.on {
  background: var(--color-primary);
}

.toggle-thumb {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: white;
  transition: left 0.2s;
}

.toggle-switch.on .toggle-thumb {
  left: 18px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 4px;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--color-primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
}

.user-details {
  display: flex;
  flex-direction: column;
}

.user-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.user-role {
  font-size: 11px;
  color: var(--text-secondary);
}
</style>
