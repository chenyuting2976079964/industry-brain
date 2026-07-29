<template>
  <div class="p-6 max-w-[1400px] mx-auto fade-in">
    <!-- Header -->
    <div class="flex items-center justify-between mb-4">
      <div>
        <h2 class="text-lg font-bold text-slate-800"><i class="fa-solid fa-chart-bar mr-2 text-amber-500"></i>产业链强弱诊断</h2>
        <p class="text-xs text-slate-400 mt-0.5">{{ pageSubtitle }}</p>
      </div>
      <button class="btn-primary text-xs" :disabled="diagnosing" @click="runDiagnosis">
        <i :class="diagnosing ? 'fa-solid fa-spinner fa-spin' : 'fa-solid fa-arrows-rotate'"></i>{{ diagnosing ? '诊断中...' : '重新诊断' }}
      </button>
    </div>

    <!-- Summary Cards -->
    <div class="grid grid-cols-4 gap-4 mb-4">
      <div class="bg-white rounded-xl p-4 border-l-4 border-emerald-500 data-card cursor-pointer hover:shadow-md" @click="filterByStatus('strong')">
        <p class="text-xs text-slate-400">强链节点</p>
        <p class="text-2xl font-bold text-slate-800 mt-1">{{ statusCounts.strong }}</p>
        <p class="text-xs text-emerald-500 mt-1"><i class="fa-solid fa-circle mr-1 text-[6px]"></i>具有竞争优势</p>
      </div>
      <div class="bg-white rounded-xl p-4 border-l-4 border-amber-500 data-card cursor-pointer hover:shadow-md" @click="filterByStatus('supplement')">
        <p class="text-xs text-slate-400">补链节点</p>
        <p class="text-2xl font-bold text-slate-800 mt-1">{{ statusCounts.supplement }}</p>
        <p class="text-xs text-amber-500 mt-1"><i class="fa-solid fa-circle mr-1 text-[6px]"></i>需加强提升</p>
      </div>
      <div class="bg-white rounded-xl p-4 border-l-4 border-red-500 data-card cursor-pointer hover:shadow-md" @click="filterByStatus('weak')">
        <p class="text-xs text-slate-400">弱链节点</p>
        <p class="text-2xl font-bold text-slate-800 mt-1">{{ statusCounts.weak }}</p>
        <p class="text-xs text-red-500 mt-1"><i class="fa-solid fa-circle mr-1 text-[6px]"></i>需重点关注</p>
      </div>
      <div class="bg-white rounded-xl p-4 border-l-4 border-slate-300 data-card cursor-pointer hover:shadow-md" @click="filterByStatus('missing')">
        <p class="text-xs text-slate-400">缺链节点</p>
        <p class="text-2xl font-bold text-slate-800 mt-1">{{ statusCounts.missing }}</p>
        <p class="text-xs text-slate-400 mt-1"><i class="fa-solid fa-circle mr-1 text-[6px]"></i>技术空白领域</p>
      </div>
    </div>

    <!-- Filter Tabs -->
    <div class="flex items-center gap-2 mb-4 flex-wrap">
      <span class="text-xs text-slate-500 mr-1">快速筛选:</span>
      <button :class="['tab-btn', { active: currentFilter === 'all' }]" @click="filterTable('all')">
        全部 <span class="ml-1 text-[10px] opacity-60">{{ diagnosisData.length }}</span>
      </button>
      <button :class="['tab-btn', { active: currentFilter === 'strong' }]" @click="filterTable('strong')">
        <span class="w-2 h-2 rounded-full bg-emerald-500 inline-block mr-1.5"></span>强链 <span class="ml-1 text-[10px] opacity-60">{{ statusCounts.strong }}</span>
      </button>
      <button :class="['tab-btn', { active: currentFilter === 'supplement' }]" @click="filterTable('supplement')">
        <span class="w-2 h-2 rounded-full bg-amber-500 inline-block mr-1.5"></span>补链 <span class="ml-1 text-[10px] opacity-60">{{ statusCounts.supplement }}</span>
      </button>
      <button :class="['tab-btn', { active: currentFilter === 'weak' }]" @click="filterTable('weak')">
        <span class="w-2 h-2 rounded-full bg-red-500 inline-block mr-1.5"></span>弱链 <span class="ml-1 text-[10px] opacity-60">{{ statusCounts.weak }}</span>
      </button>
      <button :class="['tab-btn', { active: currentFilter === 'missing' }]" @click="filterTable('missing')">
        <span class="w-2 h-2 rounded-full border-2 border-dashed border-slate-400 inline-block mr-1.5"></span>缺链 <span class="ml-1 text-[10px] opacity-60">{{ statusCounts.missing }}</span>
      </button>
      <span class="ml-auto text-xs text-slate-400">{{ filterResultText }}</span>
    </div>

    <!-- Diagnosis Table -->
    <div class="bg-white rounded-xl border border-slate-100 data-card overflow-hidden">
      <table class="table-2">
        <thead>
          <tr>
            <th>产业链节点</th>
            <th>所属层级</th>
            <th>专利数量</th>
            <th>高价值专利</th>
            <th>诊断结果</th>
            <th>专利密集度</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="node in filteredData"
            :key="node.id"
            :ref="el => setRowRef(node.id, el)"
            :data-id="node.id"
            class="cursor-pointer hover:bg-blue-50/50 node-row"
            :style="node.highlight ? { background: '#eff6ff', outline: '2px solid #3b82f6', outlineOffset: '-2px' } : {}"
            @click="openNodeDetail(node)"
          >
            <td class="font-medium">{{ node.name }} <span class="text-[10px] text-slate-400">({{ node.subtitle }})</span></td>
            <td><span :class="['badge', layerBadgeClass(node.layer)]">{{ layerNames[node.layer] }}</span></td>
            <td>{{ node.patents }}</td>
            <td>{{ node.highVal }} <span :class="['text-xs', statusTextColor(node.status)]">({{ node.highValPct }})</span></td>
            <td>
              <span :class="['badge', statusBadgeBg(node.status)]">
                <i class="fa-solid fa-circle mr-1 text-[8px]"></i>{{ statusMap[node.status] }}
              </span>
            </td>
            <td>
              <div class="flex items-center gap-2">
                <div class="w-20 h-1.5 bg-slate-100 rounded-full">
                  <div class="h-full rounded-full" :style="{ width: node.density + '%', background: densityColor(node) }"></div>
                </div>
                <span class="text-xs text-slate-400">{{ node.density }}%</span>
              </div>
            </td>
            <td>
              <button class="text-blue-600 text-xs hover:underline" @click.stop="openNodeDetail(node)">
                穿透 <i class="fa-solid fa-chevron-right ml-0.5 text-[10px]"></i>
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Slide Panel -->
    <div :class="['panel-overlay', { show: panelOpen }]" @click="closePanel"></div>
    <div :class="['slide-panel', { open: panelOpen }]">
      <div class="p-6" v-if="panelNode">
        <div class="flex items-center justify-between mb-5">
          <h3 class="text-lg font-bold text-slate-800">{{ panelNode.name }}</h3>
          <button @click="closePanel" class="w-8 h-8 rounded-lg hover:bg-slate-100 flex items-center justify-center text-slate-400">
            <i class="fa-solid fa-xmark"></i>
          </button>
        </div>
        <div class="flex items-center gap-2 mb-4">
          <span :class="['badge', statusBadgeBg(panelNode.status)]">
            <i class="fa-solid fa-circle mr-1 text-[8px]"></i>{{ statusMap[panelNode.status] }}
          </span>
        </div>
        <div class="grid grid-cols-3 gap-3 mb-4">
          <div class="bg-slate-50 rounded-lg p-4 text-center">
            <p class="text-xl font-bold text-slate-800">{{ panelNode.patents }}</p>
            <p class="text-xs text-slate-400">专利数量</p>
          </div>
          <div :class="['bg-slate-50 rounded-lg p-4 text-center']">
            <p :class="['text-xl font-bold', 'text-' + panelColor + '-600']">{{ panelNode.highVal }}</p>
            <p class="text-xs text-slate-400">高价值专利</p>
          </div>
          <div class="bg-slate-50 rounded-lg p-4 text-center">
            <p class="text-xl font-bold text-slate-800">{{ panelNode.status === 'missing' ? '—' : panelNode.density + '%' }}</p>
            <p class="text-xs text-slate-400">高价值占比</p>
          </div>
        </div>
        <h4 class="text-sm font-semibold text-slate-700 mb-2">本地主要专利持有人</h4>
        <div class="space-y-2 mb-4">
          <div
            v-for="(firm, i) in panelFirms"
            :key="i"
            class="flex items-center justify-between p-2.5 bg-slate-50 rounded-lg cursor-pointer hover:bg-blue-50"
            @click="showEnterpriseIntro(firm)"
          >
            <div class="flex items-center gap-2.5">
              <div :class="['w-8 h-8 rounded-lg flex items-center justify-center', 'bg-' + firmColors[i % 5] + '-100']">
                <span :class="['text-' + firmColors[i % 5] + '-600 font-bold text-xs']">{{ firm.charAt(0) }}</span>
              </div>
              <span class="text-sm font-medium text-slate-700">{{ firm }}</span>
            </div>
            <i class="fa-solid fa-chevron-right text-slate-300 text-xs"></i>
          </div>
        </div>
        <div class="text-center mt-4 pt-4 border-t border-slate-100">
          <button class="btn-primary justify-center text-xs" style="padding:10px 28px;font-size:13px" @click="goToInvestment">
            <i class="fa-solid fa-bullseye mr-1.5"></i>推荐招商目标
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { useEventBus } from '@/stores/eventBus'
import { getDiagnosisList, getDiagnosisNodeDetail, triggerRecalc, getDiagnosisSummary } from '@/services/diagnosis.service'
import { searchEnterprise, getEnterpriseProfile } from '@/services/enterprise.service'

const router = useRouter()
const appStore = useAppStore()
const eventBus = useEventBus()

appStore.setPage('强弱诊断', 'diagnosis')

// ─── Constants ────────────────────────────────────────────
const layerNames = { up: '上游', mid: '中游', down: '下游' }
const statusMap = { strong: '强链', supplement: '补链', weak: '弱链', missing: '缺链' }
const firmColors = ['blue', 'purple', 'emerald', 'amber', 'cyan']
const levelMap = { 1: 'up', 2: 'mid', 3: 'down' }

// ─── Data transforms ──────────────────────────────────────
function transformDiagNode(apiItem, maxPatents) {
  const pCount = apiItem.patentCount || 0
  const hvCount = apiItem.highValueCount || 0
  const density = maxPatents > 0 ? Math.round(pCount / maxPatents * 100) : 0

  return {
    id: apiItem.nodeId,
    name: apiItem.nodeName,
    subtitle: apiItem.diagnosisBasis || apiItem.nodeLevelLabel || '',
    status: apiItem.diagnosisTag || 'missing',
    layer: levelMap[apiItem.nodeLevel] || 'mid',
    patents: String(pCount),
    highVal: String(hvCount),
    highValPct: pCount > 0 ? (hvCount / pCount * 100).toFixed(1) + '%' : '0%',
    density: density,
    firms: '-',
    highlight: false,
    _raw: apiItem
  }
}

// ─── Reactive state ───────────────────────────────────────
const diagnosisData = ref([])
const currentFilter = ref('all')
const panelOpen = ref(false)
const panelNode = ref(null)
const diagnosing = ref(false)
const rowRefs = ref({})
const pageSubtitle = ref('基于高价值专利密度自动计算 · 点击节点穿透查看详情')

function setRowRef(id, el) {
  if (el) rowRefs.value[id] = el
}

// ─── Computed ──────────────────────────────────────────────
const statusCounts = computed(() => {
  const counts = { strong: 0, supplement: 0, weak: 0, missing: 0 }
  diagnosisData.value.forEach(n => { if (counts[n.status] !== undefined) counts[n.status]++ })
  return counts
})

const filteredData = computed(() => {
  if (currentFilter.value === 'all') return diagnosisData.value
  return diagnosisData.value.filter(n => n.status === currentFilter.value)
})

const filterResultText = computed(() => {
  const count = filteredData.value.length
  return currentFilter.value === 'all' ? '显示全部 ' + count + ' 个节点' : '筛选出 ' + count + ' 个节点'
})

const panelColor = computed(() => {
  if (!panelNode.value) return 'slate'
  const map = { strong: 'emerald', supplement: 'amber', weak: 'red', missing: 'slate' }
  return map[panelNode.value.status] || 'slate'
})

const panelFirms = computed(() => {
  if (!panelNode.value) return []
  const firms = panelNode.value.firms || ''
  if (firms.includes('暂无企业') || firms.includes('技术空白') || firms === '-') return []
  return firms.split('、').filter(Boolean)
})

// ─── Filter functions ─────────────────────────────────────
function filterTable(status) {
  currentFilter.value = status
}

function filterByStatus(status) {
  currentFilter.value = status
}

// ─── Badge helpers ────────────────────────────────────────
function layerBadgeClass(layer) {
  const map = { up: 'bg-blue-50 text-blue-600', mid: 'bg-cyan-50 text-cyan-600', down: 'bg-emerald-50 text-emerald-600' }
  return map[layer] || 'bg-slate-50 text-slate-500'
}

function statusBadgeBg(status) {
  const map = {
    strong: 'status-strong-bg',
    supplement: 'status-supplement-bg',
    weak: 'status-weak-bg',
    missing: 'status-missing-bg'
  }
  return map[status] || 'status-missing-bg'
}

function statusTextColor(status) {
  const map = { strong: 'text-emerald-500', supplement: 'text-amber-500', weak: 'text-red-500', missing: 'text-slate-400' }
  return map[status] || 'text-slate-400'
}

function densityColor(node) {
  const map = { strong: '#10b981', supplement: '#f59e0b', weak: '#ef4444', missing: '#cbd5e1' }
  return map[node.status] || '#cbd5e1'
}

// ─── Panel ─────────────────────────────────────────────────
function openNodeDetail(node) {
  panelNode.value = node
  panelOpen.value = true
}

function closePanel() {
  panelOpen.value = false
  panelNode.value = null
}

// ─── Enterprise intro modal ────────────────────────────────
async function showEnterpriseIntro(name) {
  try {
    const results = await searchEnterprise(name)
    if (!results || results.length === 0) {
      eventBus.openModal(name, '<p class="text-sm text-slate-500">暂无详细数据，请前往企业详情页查看</p>')
      return
    }
    const ent = results[0]
    const initial = (ent.entName || name).charAt(0)
    let profile = null
    try {
      profile = await getEnterpriseProfile(ent.id)
    } catch (e) { /* ignore */ }

    const advFields = profile?.advantages?.map(a => a.fieldName).join(' · ') || '暂无数据'
    const techFields = profile?.technologyFields?.map(t => t.fieldName).join(' · ') || '暂无数据'
    const weaknesses = profile?.weaknesses?.length > 0
      ? profile.weaknesses.map(w => w.fieldName).join(' · ')
      : '暂无数据'

    const content = `
      <div class="space-y-4">
        <div class="flex items-start gap-3">
          <div class="w-12 h-12 bg-gradient-to-br from-blue-500 to-blue-700 rounded-2xl flex items-center justify-center text-white text-lg font-bold flex-shrink-0">${initial}</div>
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2 flex-wrap"><span class="text-sm font-bold text-slate-800">${ent.entName || name}</span></div>
            <p class="text-xs text-slate-400 mt-0.5"><i class="fa-solid fa-location-dot mr-1"></i>${[ent.province, ent.city].filter(Boolean).join(' ') || '-'}</p>
          </div>
        </div>
        <div class="grid grid-cols-3 gap-3">
          <div class="bg-slate-50 rounded-lg p-3 text-center"><p class="text-lg font-bold text-blue-600">${ent.patentCount || 0}</p><p class="text-[10px] text-slate-400 mt-0.5">专利总量</p></div>
          <div class="bg-slate-50 rounded-lg p-3 text-center"><p class="text-lg font-bold text-emerald-600">${ent.enterpriseCount || 0}</p><p class="text-[10px] text-slate-400 mt-0.5">发明专利</p></div>
          <div class="bg-slate-50 rounded-lg p-3 text-center"><p class="text-lg font-bold text-amber-600">${ent.highValueCount || 0}</p><p class="text-[10px] text-slate-400 mt-0.5">高价值专利</p></div>
        </div>
        <div><p class="text-xs font-medium text-slate-700 mb-2"><i class="fa-solid fa-microchip mr-1 text-blue-500"></i>技术优势</p><p class="text-xs text-slate-600">${advFields}</p></div>
        <div><p class="text-xs font-medium text-slate-700 mb-2"><i class="fa-solid fa-tags mr-1 text-blue-500"></i>研发方向</p><p class="text-xs text-slate-600">${techFields}</p></div>
        <div class="bg-red-50 rounded-lg p-3 border border-red-100 flex items-start gap-2"><i class="fa-solid fa-triangle-exclamation text-red-400 text-xs mt-0.5"></i><div><p class="text-xs font-medium text-slate-700">技术短板</p><p class="text-xs text-slate-500 mt-0.5">${weaknesses}</p></div></div>
        <button class="btn-outline justify-center text-xs w-full" onclick="document.querySelector('.panel-overlay')?.click?.()"><i class="fa-regular fa-circle-check mr-1"></i>关闭</button>
      </div>`

    eventBus.openModal(ent.entName + ' · 企业画像', content)
  } catch (e) {
    eventBus.openModal(name, '<p class="text-sm text-slate-500">加载失败，请稍后重试</p>')
  }
}

// ─── Diagnosis trigger ─────────────────────────────────────
async function runDiagnosis() {
  diagnosing.value = true
  try {
    const result = await triggerRecalc()
    const taskId = result.taskId

    // Poll for completion
    const poll = setInterval(async () => {
      try {
        const status = await getRecalcStatus(taskId)
        if (status.status === 'completed' || status.status === 'failed') {
          clearInterval(poll)
          diagnosing.value = false
          if (status.status === 'completed') {
            eventBus.openModal('诊断完成', `
              <div class="text-center py-4">
                <i class="fa-solid fa-circle-check text-emerald-500 text-4xl mb-3"></i>
                <p class="text-sm text-slate-600">产业链强弱诊断已完成！</p>
                <p class="text-xs text-slate-400 mt-1">${status.message || '数据已更新'}</p>
              </div>`)
            // Reload data
            await loadDiagnosisData()
          } else {
            eventBus.openModal('诊断失败', `
              <div class="text-center py-4">
                <i class="fa-solid fa-circle-exclamation text-red-500 text-4xl mb-3"></i>
                <p class="text-sm text-slate-600">诊断任务执行失败</p>
                <p class="text-xs text-slate-400 mt-1">${status.message || '请稍后重试'}</p>
              </div>`)
          }
        }
      } catch (e) { /* ignore poll errors */ }
    }, 2000)

    // Safety timeout after 60 seconds
    setTimeout(() => {
      clearInterval(poll)
      if (diagnosing.value) {
        diagnosing.value = false
        eventBus.openModal('诊断超时', `
          <div class="text-center py-4">
            <i class="fa-solid fa-clock text-amber-500 text-4xl mb-3"></i>
            <p class="text-sm text-slate-600">诊断任务处理时间较长</p>
            <p class="text-xs text-slate-400 mt-1">请稍后刷新页面查看结果</p>
          </div>`)
      }
    }, 60000)
  } catch (e) {
    diagnosing.value = false
    eventBus.openModal('诊断失败', `
      <div class="text-center py-4">
        <i class="fa-solid fa-circle-exclamation text-red-500 text-4xl mb-3"></i>
        <p class="text-sm text-slate-600">无法启动诊断任务</p>
        <p class="text-xs text-slate-400 mt-1">${e.message || '请检查网络后重试'}</p>
      </div>`)
  }
}

// ─── Data loading ─────────────────────────────────────────
async function loadDiagnosisData(filterStatus = 'all') {
  try {
    const items = await getDiagnosisList(filterStatus === 'all' ? undefined : filterStatus)
    const maxPatents = (items || []).reduce((max, item) => Math.max(max, item.patentCount || 0), 0)
    diagnosisData.value = (items || []).map(item => transformDiagNode(item, maxPatents))
  } catch (e) {
    console.error('Failed to load diagnosis data', e)
    diagnosisData.value = []
  }
}

// ─── Navigation ───────────────────────────────────────────
function goToInvestment() {
  if (panelNode.value) {
    sessionStorage.setItem('navContext', JSON.stringify({ fromNode: panelNode.value.name }))
  }
  closePanel()
  router.push('/investment')
}

// ─── Handle focusNode from industry page ──────────────────
onMounted(async () => {
  await loadDiagnosisData()

  nextTick(() => {
    try {
      const ctx = JSON.parse(sessionStorage.getItem('navContext')) || {}
      if (ctx.focusNode) {
        pageSubtitle.value = '正在查看"' + ctx.focusNode + '"的诊断详情 · 点击节点穿透查看企业'
        const node = diagnosisData.value.find(n => n.name === ctx.focusNode)
        if (node) {
          node.highlight = true
          const row = rowRefs.value[node.id]
          if (row) {
            // Vue 3 ref on component: scroll into view
            if (row.$el) {
              row.$el.scrollIntoView({ behavior: 'smooth', block: 'center' })
            } else if (row instanceof HTMLElement) {
              row.scrollIntoView({ behavior: 'smooth', block: 'center' })
            }
          }
        }
        sessionStorage.removeItem('navContext')
      }
    } catch (e) { /* ignore */ }
  })
})
</script>

<style scoped>
.slide-panel {
  position: fixed;
  top: 0;
  right: -520px;
  width: 520px;
  height: 100%;
  background: #fff;
  z-index: 100;
  box-shadow: -4px 0 30px rgba(0, 0, 0, .12);
  transition: right .3s ease;
  overflow-y: auto;
}
.slide-panel.open { right: 0; }

.panel-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, .3);
  z-index: 99;
  display: none;
}
.panel-overlay.show { display: block; }

.tab-btn {
  padding: 8px 20px;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  transition: all .2s;
  border: none;
  background: transparent;
  color: #64748b;
}
.tab-btn.active { background: #1e40af; color: #fff; box-shadow: 0 2px 8px rgba(30, 64, 175, .3); }
.tab-btn:hover:not(.active) { background: #e2e8f0; }

.status-strong-bg { background: #d1fae5; color: #065f46; }
.status-supplement-bg { background: #fef3c7; color: #92400e; }
.status-weak-bg { background: #fee2e2; color: #991b1b; }
.status-missing-bg { background: #f1f5f9; color: #475569; }

.badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 500;
}

.table-2 { width: 100%; border-collapse: collapse; }
.table-2 th {
  padding: 10px 12px;
  text-align: left;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  border-bottom: 2px solid #e2e8f0;
}
.table-2 td { padding: 10px 12px; font-size: 13px; border-bottom: 1px solid #e2e8f0; }
.table-2 tr:hover { background: #f8fafc; }

.btn-primary {
  background: #1e40af;
  color: #fff;
  padding: 8px 20px;
  border-radius: 8px;
  font-size: 13px;
  border: none;
  cursor: pointer;
  transition: all .2s;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.btn-primary:hover { background: #1e3a8a; box-shadow: 0 4px 12px rgba(30, 64, 175, .3); }
.btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }

.btn-outline {
  border: 1px solid #e2e8f0;
  background: #fff;
  color: #1e293b;
  padding: 8px 20px;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  transition: all .2s;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.btn-outline:hover { border-color: #1e40af; color: #1e40af; }

.data-card { transition: all .2s; }
.data-card:hover { box-shadow: 0 4px 15px rgba(0, 0, 0, .08); }
</style>
