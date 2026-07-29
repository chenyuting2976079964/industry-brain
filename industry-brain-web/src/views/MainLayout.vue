<template>
  <div class="flex h-screen overflow-hidden">
    <!-- ==================== SIDEBAR ==================== -->
    <aside class="sidebar">
      <!-- Logo -->
      <div class="sidebar-logo">
        <div class="sidebar-logo-icon">
          <img src="/logo_white.png" alt="" class="sidebar-logo-img" />
        </div>
        <div>
          <p class="sidebar-logo-title">产业大脑</p>
          <p class="sidebar-logo-ver">决策引擎 v2.0</p>
        </div>
      </div>

      <!-- Nav -->
      <nav class="sidebar-nav">
        <router-link to="/home" class="sidebar-link" active-class="active">
          <i class="fa-solid fa-house"></i>首页看板
        </router-link>
        <router-link to="/local-enterprises" class="sidebar-link" active-class="active">
          <i class="fa-solid fa-building"></i>查看本地企业
        </router-link>
        <router-link to="/industry" class="sidebar-link" active-class="active">
          <i class="fa-solid fa-sitemap"></i>产业链全景
        </router-link>
        <router-link to="/diagnosis" class="sidebar-link" active-class="active">
          <i class="fa-solid fa-chart-bar"></i>强弱诊断
        </router-link>
        <router-link to="/investment" class="sidebar-link" active-class="active">
          <i class="fa-solid fa-bullseye"></i>精准招商
        </router-link>
        <router-link to="/reports" class="sidebar-link" active-class="active">
          <i class="fa-regular fa-file-lines"></i>招商报告
        </router-link>
      </nav>

      <!-- User -->
      <div class="sidebar-user">
        <div class="sidebar-user-row">
          <div class="sidebar-user-avatar">{{ userInitial }}</div>
          <div class="sidebar-user-info">
            <p class="sidebar-user-name">{{ userName }}</p>
            <p class="sidebar-user-role">{{ userRole }}</p>
          </div>
          <button @click="handleLogout" class="sidebar-logout-btn" title="退出登录">
            <i class="fa-solid fa-right-from-bracket"></i>
          </button>
        </div>
      </div>
    </aside>

    <!-- ==================== MAIN AREA ==================== -->
    <div class="main-area">
      <!-- Top Bar -->
      <header class="topbar">
        <div class="topbar-left">
          <h1 class="topbar-title">{{ pageTitle }}</h1>
          <span class="topbar-dot pulse-dot"></span>
          <span class="topbar-status">系统运行中</span>
        </div>
        <div class="topbar-right">
          <span><i class="fa-regular fa-clock mr-1"></i>{{ timeDisplay }}</span>
          <span class="topbar-divider"></span>
          <span><i class="fa-regular fa-bell mr-1"></i>{{ alertCount }}</span>
        </div>
      </header>

      <!-- Page Content -->
      <div class="page-content">
        <router-view v-slot="{ Component }">
          <Transition name="page" mode="out-in">
            <component :is="Component" />
          </Transition>
        </router-view>
      </div>
    </div>

    <!-- ==================== GLOBAL MODAL ==================== -->
    <Teleport to="body">
      <div v-if="eventBus.modalVisible" class="modal-overlay" @click.self="eventBus.closeModal()">
        <div class="modal-container modal-enter">
          <div class="modal-header">
            <h3 class="modal-title">{{ eventBus.modalTitle }}</h3>
            <button @click="eventBus.closeModal()" class="modal-close-btn">
              <i class="fa-solid fa-xmark"></i>
            </button>
          </div>
          <div class="modal-body" v-html="eventBus.modalContent"></div>
        </div>
      </div>

      <!-- ==================== FULLSCREEN DASHBOARD OVERLAY ==================== -->
      <div v-if="appStore.showDashOverlay" class="dash-overlay" ref="dashOverlayRef">
        <button @click="exitDashboard" class="dash-close-btn">
          <i class="fa-solid fa-xmark"></i>
        </button>
        <DashboardFullView />
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { useEventBus } from '@/stores/eventBus'
import DashboardFullView from '@/views/DashboardFullView.vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const appStore = useAppStore()
const eventBus = useEventBus()

const dashOverlayRef = ref(null)

// ---- Computed ----
const pageTitle = computed(() => appStore.pageTitle)
const alertCount = computed(() => appStore.alertCount)
const userName = computed(() => authStore.user?.displayName || '管理员')
const userRole = computed(() => authStore.user?.role || '园区运营')
const userInitial = computed(() => userName.value.charAt(0))

// Real-time clock, synced to system time
const now = ref(new Date())
const timeDisplay = computed(() => {
  const y = now.value.getFullYear()
  const mo = (now.value.getMonth() + 1).toString().padStart(2, '0')
  const d = now.value.getDate().toString().padStart(2, '0')
  const h = now.value.getHours().toString().padStart(2, '0')
  const mi = now.value.getMinutes().toString().padStart(2, '0')
  return `${y}-${mo}-${d} ${h}:${mi}`
})
let clockTimer = null

// ---- Watch route to update page title ----
watch(() => route.meta.title, (title) => {
  if (title) {
    appStore.setPage(title, route.name?.toLowerCase() || 'home')
  }
}, { immediate: true })

// ---- Init ----
onMounted(async () => {
  // Real-time clock
  clockTimer = setInterval(() => { now.value = new Date() }, 1000)

  // Fetch current user info (A03)
  try {
    await authStore.fetchCurrentUser()
  } catch (e) {
    handleLogout()
  }

  // Listen to fullscreen events
  document.addEventListener('fullscreenchange', onFullscreenChange)
  document.addEventListener('keydown', onKeyDown)

  // Event bus listeners for cross-page communication
  eventBus.on('navigate', handleEventNavigate)
  eventBus.on('showDash', () => appStore.enterDashboard())
  eventBus.on('exitDash', () => exitDashboard())
  eventBus.on('openModal', ({ title, content }) => eventBus.openModal(title, content))
  eventBus.on('closeModal', () => eventBus.closeModal())
})

onUnmounted(() => {
  clearInterval(clockTimer)
  document.removeEventListener('fullscreenchange', onFullscreenChange)
  document.removeEventListener('keydown', onKeyDown)
})

// ---- Fullscreen Dashboard ----
function enterDashboard() {
  appStore.enterDashboard()
  setTimeout(() => {
    try { dashOverlayRef.value?.requestFullscreen() } catch (e) { /* ok */ }
  }, 500)
}

function exitDashboard() {
  appStore.exitDashboard()
  try {
    if (document.fullscreenElement) document.exitFullscreen()
  } catch (e) { /* ok */ }
}

function onFullscreenChange() {
  if (!document.fullscreenElement && appStore.showDashOverlay) {
    appStore.exitDashboard()
  }
}

function onKeyDown(e) {
  if (e.key === 'Escape' && appStore.showDashOverlay) {
    exitDashboard()
  }
}

// ---- Event Bus Handlers ----
function handleEventNavigate({ page, context }) {
  if (context) {
    sessionStorage.setItem('navContext', JSON.stringify(context))
  }
  router.push(`/${page}`)
}

// ---- Logout ----
async function handleLogout() {
  await authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
/* ========== Layout ========== */
.sidebar {
  width: 224px;
  background: #1e3a8a;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

/* ========== Sidebar Logo ========== */
.sidebar-logo {
  height: 56px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 20px;
  border-bottom: 1px solid rgba(255,255,255,.1);
}
.sidebar-logo-icon {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #22d3ee, #2563eb);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.sidebar-logo-img {
  width: 24px;
  height: 24px;
  object-fit: contain;
}
.sidebar-logo-title {
  color: #fff;
  font-size: 0.875rem;
  font-weight: 700;
  line-height: 1.25;
}
.sidebar-logo-ver {
  color: rgba(147, 197, 253, 0.4);
  font-size: 0.625rem;
  line-height: 1.25;
}

/* ========== Sidebar Nav ========== */
.sidebar-nav {
  flex: 1;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  overflow-y: auto;
}

/* ========== Sidebar User ========== */
.sidebar-user {
  padding: 12px;
  border-top: 1px solid rgba(255,255,255,.1);
}
.sidebar-user-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
}
.sidebar-user-row:hover {
  background: rgba(255,255,255,.05);
}
.sidebar-user-avatar {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #60a5fa, #a855f7);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 0.75rem;
  font-weight: 700;
  flex-shrink: 0;
}
.sidebar-user-info {
  flex: 1;
  min-width: 0;
}
.sidebar-user-name {
  color: #fff;
  font-size: 0.75rem;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.sidebar-user-role {
  color: rgba(147, 197, 253, 0.5);
  font-size: 0.625rem;
}
.sidebar-logout-btn {
  color: rgba(147, 197, 253, 0.4);
  background: none;
  border: none;
  cursor: pointer;
  font-size: 0.75rem;
  padding: 4px;
  transition: color 0.15s;
}
.sidebar-logout-btn:hover {
  color: #f87171;
}

/* ========== Topbar ========== */
.topbar {
  height: 56px;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  flex-shrink: 0;
}
.topbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.topbar-title {
  font-size: 0.875rem;
  font-weight: 700;
  color: #1e293b;
}
.topbar-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #10b981;
}
.topbar-status {
  font-size: 0.75rem;
  color: #94a3b8;
}
.topbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 0.75rem;
  color: #94a3b8;
}
.topbar-divider {
  width: 1px;
  height: 16px;
  background: #e2e8f0;
}

/* ========== Page Content ========== */
.page-content {
  flex: 1;
  background: #f1f5f9;
  overflow: auto;
}

/* ========== Page Transition ========== */
.page-enter-active {
  animation: fadeIn 0.25s ease;
}
.page-leave-active {
  animation: fadeIn 0.15s ease reverse;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* ========== Global Modal ========== */
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0,0,0,.4);
}
.modal-container {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0,0,0,.15);
  max-width: 42rem;
  width: calc(100% - 2rem);
  max-height: 80vh;
  overflow-y: auto;
}
.modal-header {
  position: sticky;
  top: 0;
  background: #fff;
  border-bottom: 1px solid #f1f5f9;
  padding: 1rem 1.5rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-radius: 16px 16px 0 0;
}
.modal-title {
  font-weight: 600;
  color: #1e293b;
  font-size: 0.9375rem;
}
.modal-close-btn {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: none;
  background: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 1rem;
  transition: background 0.15s;
}
.modal-close-btn:hover {
  background: #f1f5f9;
}
.modal-body {
  padding: 1.5rem;
}

/* ========== Dashboard Overlay ========== */
.dash-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: #f1f5f9;
}
.dash-close-btn {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 10000;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: rgba(255,255,255,.9);
  border: 1px solid #e2e8f0;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  font-size: 1rem;
  box-shadow: 0 2px 8px rgba(0,0,0,.08);
}
.dash-close-btn:hover {
  background: #fff;
  color: #1e293b;
}
.dash-frame {
  width: 100%;
  height: 100%;
  border: none;
}
</style>
