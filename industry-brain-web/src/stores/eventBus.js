import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * Event bus store — replaces the prototype's postMessage architecture.
 * All cross-page communication flows through this store.
 *
 * Events:
 *  - navigate { page, context? }
 *  - openModal { title, content }
 *  - closeModal
 *  - showDash
 *  - exitDash
 *  - openEnterpriseList { category }
 *  - showEnterpriseIntro { id }
 *  - showParkEnterpriseList { park }
 *  - showParkPatentDetail { park }
 *  - showParkHighValueDetail { park }
 */
export const useEventBus = defineStore('eventBus', () => {
  const modalVisible = ref(false)
  const modalTitle = ref('')
  const modalContent = ref('')

  // Listeners map: eventType -> [callbacks]
  const listeners = new Map()

  function on(event, callback) {
    if (!listeners.has(event)) listeners.set(event, [])
    listeners.get(event).push(callback)
    return () => {
      const arr = listeners.get(event)
      if (arr) {
        const idx = arr.indexOf(callback)
        if (idx > -1) arr.splice(idx, 1)
      }
    }
  }

  function emit(event, payload) {
    const arr = listeners.get(event)
    if (arr) arr.forEach(fn => fn(payload))
  }

  function openModal(title, content) {
    modalTitle.value = title
    modalContent.value = content
    modalVisible.value = true
  }

  function closeModal() {
    modalVisible.value = false
    modalTitle.value = ''
    modalContent.value = ''
  }

  return { modalVisible, modalTitle, modalContent, on, emit, openModal, closeModal }
})
