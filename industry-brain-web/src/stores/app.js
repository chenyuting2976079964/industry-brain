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

  function enterDashboard() {
    showDashOverlay.value = true
  }

  function exitDashboard() {
    showDashOverlay.value = false
  }

  return { pageTitle, currentPage, alertCount, lastUpdateTime, showDashOverlay, setPage, enterDashboard, exitDashboard }
})
