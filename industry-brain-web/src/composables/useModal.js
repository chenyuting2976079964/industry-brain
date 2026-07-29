/**
 * useModal — 弹窗组合式函数
 *
 * 封装全局弹窗的打开/关闭和按钮事件绑定。
 * 协作者只需传 { 选择器 → 回调 }，不用手写 setTimeout + querySelector。
 *
 * 用法:
 *   const modal = useModal()
 *
 *   // 简单弹窗（无交互按钮）
 *   modal.open('标题', '<p>内容</p>')
 *
 *   // 带交互按钮 — data-action 属性自动绑定
 *   modal.open('标题', `
 *     <button data-action="save">保存</button>
 *     <button data-action="cancel">取消</button>
 *   `, {
 *     save: () => { ... },       // 等价于 [data-action="save"]
 *     cancel: () => { ... }      // 等价于 [data-action="cancel"]
 *   })
 *
 *   // 也可以直接用 CSS 选择器
 *   modal.open('标题', html, {
 *     '#btnId': (el) => { ... }  // el 是当前点击的元素
 *   })
 *
 *   // 关闭
 *   modal.close()
 */

import { useEventBus } from '@/stores/eventBus'

export function useModal() {
  const eventBus = useEventBus()

  /**
   * 打开弹窗，自动绑定交互按钮事件
   * @param {string} title
   * @param {string} html
   * @param {Record<string, Function>} actions — key 可以是 'actionName'（匹配 data-action）或 CSS 选择器
   */
  function open(title, html, actions = {}) {
    eventBus.openModal(title, html)
    if (Object.keys(actions).length === 0) return

    setTimeout(() => {
      for (const [key, handler] of Object.entries(actions)) {
        // 如果 key 不含特殊字符，当作 data-action 的值来匹配
        const selector = /^[a-zA-Z][\w-]*$/.test(key) ? `[data-action="${key}"]` : key
        const els = document.querySelectorAll(selector)
        els.forEach(el => {
          el.addEventListener('click', (e) => handler(el, e), { once: true })
        })
      }
    }, 100)
  }

  /** 关闭弹窗 */
  function close() {
    eventBus.closeModal()
  }

  return { open, close }
}
