import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const pageTitle = ref('首页看板')
  const currentPage = ref('home')
  const alertCount = ref(0)
  const lastUpdateTime = ref((() => {
    const t = new Date()
    const pad = n => n.toString().padStart(2, '0')
    return `${t.getFullYear()}-${pad(t.getMonth() + 1)}-${pad(t.getDate())} ${pad(t.getHours())}:${pad(t.getMinutes())}`
  })())
  const showDashOverlay = ref(false)

  function setPage(title, page) {
    pageTitle.value = title
    currentPage.value = page
  }

  async function enterDashboard() {
    showDashOverlay.value = true
    await new Promise(r => setTimeout(r, 200))
    try {
      const el = document.documentElement
      if (el.requestFullscreen) {
        await el.requestFullscreen()
      } else if (el.webkitRequestFullscreen) {
        el.webkitRequestFullscreen()
      }
    } catch (e) {
      // 全屏失败也没关系，overlay 仍然可见
    }
  }

  function exitDashboard() {
    try { if (document.fullscreenElement) document.exitFullscreen() } catch (e) {}
    showDashOverlay.value = false
  }

  if (typeof document !== 'undefined') {
    const onFs = () => {
      if (!document.fullscreenElement && !document.webkitFullscreenElement) {
        showDashOverlay.value = false
      }
    }
    document.addEventListener('fullscreenchange', onFs)
    document.addEventListener('webkitfullscreenchange', onFs)
  }

  return { pageTitle, currentPage, alertCount, lastUpdateTime, showDashOverlay, setPage, enterDashboard, exitDashboard }
})
