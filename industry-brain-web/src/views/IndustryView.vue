<template>
  <div class="p-6 max-w-[1400px] mx-auto fade-in">
    <!-- Header + Industry Selector -->
    <div class="bg-white rounded-xl p-4 border border-slate-100 shadow-sm mb-4">
      <div class="flex items-center justify-between mb-3">
        <div class="flex items-center gap-3">
          <h2 class="text-base font-bold text-slate-800"><i class="fa-solid fa-sitemap mr-2 text-blue-600"></i>产业链全景图谱</h2>
          <span class="text-xs text-slate-400">{{ chainMeta }}</span>
        </div>
        <div class="flex items-center gap-3 text-xs">
          <span class="flex items-center gap-1"><span class="w-2.5 h-2.5 rounded-full bg-emerald-500"></span>强链</span>
          <span class="flex items-center gap-1"><span class="w-2.5 h-2.5 rounded-full bg-amber-500"></span>补链</span>
          <span class="flex items-center gap-1"><span class="w-2.5 h-2.5 rounded-full bg-red-500"></span>弱链</span>
          <span class="flex items-center gap-1"><span class="w-2.5 h-2.5 rounded-full border-2 border-dashed border-slate-400 bg-white"></span>缺链</span>
        </div>
      </div>
      <div class="flex items-center gap-2 overflow-x-auto scrollbar-hide pb-0.5">
        <button
          v-for="chain in chainList"
          :key="chain.key"
          :class="['industry-tab', { active: currentChain === chain.key }]"
          @click="switchChain(chain.key)"
        >{{ chain.icon }} {{ chain.label }}</button>
      </div>
    </div>

    <div class="grid grid-cols-[1fr_280px] gap-4">
      <!-- Graph Area -->
      <div class="bg-white rounded-xl border border-slate-100 shadow-sm overflow-hidden">
        <div class="text-center py-3 border-b border-slate-100 bg-slate-50/50">
          <h3 class="text-sm font-bold text-slate-700">{{ chainTitle }}</h3>
          <p class="text-[11px] text-slate-400 mt-0.5">{{ chainSubtitle }}</p>
        </div>
        <div class="relative" ref="chainContent" style="min-height:580px">
          <div class="graph-container" :style="{ display: 'flex', gap: 0, position: 'relative', minHeight: '560px' }">
            <div
              v-for="(layer, li) in layers"
              :key="layer.key"
              :data-layer="layer.key"
              :style="{ width: li === 1 ? '38%' : '31%', flexShrink: 0, borderRight: li < 2 ? '1px solid #e2e8f0' : 'none' }"
            >
              <div class="text-center py-2 text-xs font-semibold" :style="{ background: layer.headerBg, color: layer.headerColor, borderBottom: '2px solid ' + layer.headerColor }">
                <i :class="layer.icon + ' mr-1.5'"></i>{{ layer.label }}
              </div>
              <div class="flex flex-col items-center gap-3 py-4 px-2">
                <div
                  v-for="node in getNodesByLayer(layer.key)"
                  :key="node.id"
                  :ref="el => setNodeRef(node.id, el)"
                  class="graph-node flex flex-col items-center"
                  :data-id="node.id"
                  :data-name="node.name"
                  :data-layer="node.layer"
                  :data-status="node.status"
                  :style="{ opacity: node.filtered ? 0.18 : 1, pointerEvents: node.filtered ? 'none' : '' }"
                  @click="selectNode(node.id)"
                >
                  <div
                    class="flex items-center justify-center rounded-full node-ring"
                    :style="{
                      width: nodeSize(node) + 'px',
                      height: nodeSize(node) + 'px',
                      border: '3px ' + (node.status === 'missing' ? 'dashed' : 'solid') + ' ' + statusColors[node.status].border,
                      background: statusColors[node.status].bg
                    }"
                    :title="node.desc"
                  >
                    <div class="text-center leading-tight px-0.5">
                      <p class="font-bold text-slate-800" :style="{ fontSize: nodeSize(node) > 80 ? '11px' : '9px' }">{{ node.name }}</p>
                      <p class="text-slate-400" :style="{ fontSize: nodeSize(node) > 80 ? '9px' : '8px' }">{{ node.enterprises }}家</p>
                    </div>
                  </div>
                  <span
                    class="mt-1 text-[9px] px-1.5 py-0.5 rounded-full font-medium"
                    :style="{ background: statusColors[node.status].bg, color: statusColors[node.status].text }"
                  >{{ statusMap[node.status] }}</span>
                </div>
              </div>
            </div>
            <svg class="link-svg" :style="{ position: 'absolute', top: 0, left: 0, width: '100%', height: '100%', pointerEvents: 'none', zIndex: 1 }">
              <defs>
                <marker id="arrowHead" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="6" markerHeight="6" orient="auto">
                  <path d="M0,0 L10,5 L0,10 Z" fill="#94a3b8" opacity="0.5" />
                </marker>
                <marker id="arrowHeadDim" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="6" markerHeight="6" orient="auto">
                  <path d="M0,0 L10,5 L0,10 Z" fill="#e2e8f0" opacity="0.3" />
                </marker>
              </defs>
              <path
                v-for="(link, idx) in visibleLinks"
                :key="idx"
                :d="link.d"
                :stroke="link.active ? '#94a3b8' : '#e2e8f0'"
                :stroke-width="link.active ? 1.5 : 1"
                fill="none"
                :opacity="link.active ? 0.5 : 0.15"
                stroke-linecap="round"
                :marker-end="link.active ? 'url(#arrowHead)' : 'url(#arrowHeadDim)'"
              />
            </svg>
          </div>
        </div>
      </div>

      <!-- Right Panel -->
      <div class="space-y-4">
        <div class="bg-white rounded-xl p-4 border border-slate-100 shadow-sm">
          <h4 class="text-sm font-semibold text-slate-700 mb-3"><i class="fa-solid fa-chart-simple mr-2 text-blue-500"></i>诊断概览</h4>
          <div class="space-y-2">
            <div class="flex items-center justify-between p-2.5 bg-emerald-50 rounded-lg">
              <span class="text-xs text-emerald-700 font-medium">强链</span>
              <span class="text-sm font-bold text-emerald-600">{{ statusCounts.strong }}</span>
            </div>
            <div class="flex items-center justify-between p-2.5 bg-amber-50 rounded-lg">
              <span class="text-xs text-amber-700 font-medium">补链</span>
              <span class="text-sm font-bold text-amber-600">{{ statusCounts.supplement }}</span>
            </div>
            <div class="flex items-center justify-between p-2.5 bg-red-50 rounded-lg">
              <span class="text-xs text-red-700 font-medium">弱链</span>
              <span class="text-sm font-bold text-red-600">{{ statusCounts.weak }}</span>
            </div>
            <div class="flex items-center justify-between p-2.5 bg-slate-50 rounded-lg">
              <span class="text-xs text-slate-600 font-medium">缺链</span>
              <span class="text-sm font-bold text-slate-500">{{ statusCounts.missing }}</span>
            </div>
          </div>
        </div>
        <div class="bg-white rounded-xl p-4 border border-slate-100 shadow-sm">
          <h4 class="text-sm font-semibold text-slate-700 mb-3"><i class="fa-solid fa-filter mr-2 text-slate-400"></i>快速筛选</h4>
          <div class="space-y-2">
            <select v-model="layerFilter" class="select-field text-xs" @change="applyFilters">
              <option value="all">全部层级</option>
              <option value="up">上游</option>
              <option value="mid">中游</option>
              <option value="down">下游</option>
            </select>
            <select v-model="statusFilter" class="select-field text-xs" @change="applyFilters">
              <option value="all">全部状态</option>
              <option value="strong">强链</option>
              <option value="supplement">补链</option>
              <option value="weak">弱链</option>
              <option value="missing">缺链</option>
            </select>
            <input v-model="searchQuery" class="input-field text-xs" placeholder="搜索节点..." @input="applyFilters" />
            <button class="btn-outline w-full justify-center text-xs" @click="resetFilters"><i class="fa-solid fa-rotate"></i> 重置</button>
          </div>
        </div>
        <div class="bg-blue-50 rounded-xl p-3 border border-blue-100 text-xs text-blue-700 leading-relaxed">
          <i class="fa-regular fa-lightbulb mr-1"></i>节点间连线表示技术供应/上下游配套关系。点击任意节点查看专利详情及招商匹配。
        </div>
      </div>
    </div>

    <!-- Slide Panel -->
    <div :class="['panel-overlay', { show: panelOpen }]" @click="closePanel"></div>
    <div :class="['slide-panel', { open: panelOpen }]">
      <div class="p-6" v-if="selectedNode">
        <div class="flex items-center justify-between mb-5">
          <h3 class="text-lg font-bold text-slate-800">{{ selectedNode.name }}</h3>
          <button @click="closePanel" class="w-8 h-8 rounded-lg hover:bg-slate-100 flex items-center justify-center text-slate-400">
            <i class="fa-solid fa-xmark"></i>
          </button>
        </div>
        <div class="flex items-center gap-2 mb-4">
          <span class="badge" :style="{ background: nodeStatusColor.bg, color: nodeStatusColor.text }">
            <i class="fa-solid fa-circle mr-1 text-[8px]"></i>{{ statusMap[selectedNode.status] }}
          </span>
          <span class="badge bg-blue-50 text-blue-600">{{ layerNames[selectedNode.layer] }}</span>
          <span class="badge bg-slate-50 text-slate-500">{{ selectedNode.desc }}</span>
        </div>
        <div class="grid grid-cols-3 gap-3 mb-4">
          <div class="bg-slate-50 rounded-lg p-4 text-center">
            <p class="text-xl font-bold text-slate-800">{{ selectedNode.enterprises }}</p>
            <p class="text-xs text-slate-400">企业数量</p>
          </div>
          <div class="bg-slate-50 rounded-lg p-4 text-center">
            <p class="text-xl font-bold" :style="{ color: nodeStatusColor.border }">{{ selectedNode.highVal }}</p>
            <p class="text-xs text-slate-400">高价值专利</p>
          </div>
          <div class="bg-slate-50 rounded-lg p-4 text-center">
            <p class="text-xl font-bold text-slate-800">{{ highValueRatio }}</p>
            <p class="text-xs text-slate-400">高价值占比</p>
          </div>
        </div>
        <h4 class="text-sm font-semibold text-slate-700 mb-2">主要企业</h4>
        <div class="space-y-2 mb-4">
          <div v-if="selectedNode.firms === '（技术空白）'" class="p-3 bg-slate-50 rounded-lg text-center text-xs text-slate-400">
            暂无企业布局
          </div>
          <div
            v-for="(firm, i) in firmList"
            :key="i"
            class="flex items-center justify-between p-2.5 bg-slate-50 rounded-lg cursor-pointer hover:bg-blue-50"
            @click="goToEnterprise(firm)"
          >
            <div class="flex items-center gap-2.5">
              <div :class="['w-8 h-8 rounded-lg flex items-center justify-center', 'bg-' + firmColors[i % 7] + '-100']">
                <span :class="['text-' + firmColors[i % 7] + '-600 font-bold text-xs']">{{ firm.charAt(0) }}</span>
              </div>
              <span class="text-sm font-medium text-slate-700">{{ firm }}</span>
            </div>
            <i class="fa-solid fa-chevron-right text-slate-300 text-xs"></i>
          </div>
        </div>
        <div class="border-t border-slate-100 pt-4 mt-2 flex gap-2">
          <button class="btn-primary flex-1 justify-center text-xs" @click="goToDiagnosis">
            <i class="fa-solid fa-chart-bar"></i> 诊断详情
          </button>
          <button class="btn-outline flex-1 justify-center text-xs" @click="goToInvestment">
            <i class="fa-solid fa-bullseye"></i> 招商匹配
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { useEventBus } from '@/stores/eventBus'
import { getChainList, getChainNodes, getChainLinks, getNodeDetail } from '@/services/industryChain.service'

const router = useRouter()
const appStore = useAppStore()
const eventBus = useEventBus()

appStore.setPage('产业链全景', 'industry')

// ─── Constants ────────────────────────────────────────────
const layerNames = { up: '上游', mid: '中游', down: '下游' }
const statusMap = { strong: '强链', supplement: '补链', weak: '弱链', missing: '缺链' }
const statusColors = {
  strong: { bg: '#ecfdf5', border: '#10b981', text: '#065f46' },
  supplement: { bg: '#fffbeb', border: '#f59e0b', text: '#92400e' },
  weak: { bg: '#fef2f2', border: '#ef4444', text: '#991b1b' },
  missing: { bg: '#f8fafc', border: '#cbd5e1', text: '#64748b' }
}

const layers = [
  { key: 'up', label: '上游', headerColor: '#2563eb', headerBg: '#eff6ff', icon: 'fa-solid fa-arrow-trend-up' },
  { key: 'mid', label: '中游', headerColor: '#0891b2', headerBg: '#ecfeff', icon: 'fa-solid fa-microchip' },
  { key: 'down', label: '下游', headerColor: '#059669', headerBg: '#ecfdf5', icon: 'fa-solid fa-rocket' }
]

const firmColors = ['blue', 'purple', 'emerald', 'amber', 'cyan', 'indigo', 'rose']

const levelMap = { 1: 'up', 2: 'mid', 3: 'down' }

function getChainIcon(name) {
  if (!name) return '🔗'
  if (name.includes('智能')) return '🏭'
  if (name.includes('汽车')) return '🚗'
  if (name.includes('集成') || name.includes('芯片')) return '💻'
  if (name.includes('医药') || name.includes('生物')) return '💊'
  if (name.includes('人工')) return '🤖'
  return '🔗'
}

// ─── Data transforms ──────────────────────────────────────
function transformNode(apiNode) {
  return {
    id: apiNode.id,
    name: apiNode.nodeName,
    status: apiNode.diagnosisTag || 'missing',
    enterprises: String(apiNode.enterpriseCount ?? 0),
    patents: String(apiNode.patentCount ?? 0),
    highVal: String(apiNode.highValueCount ?? 0),
    layer: levelMap[apiNode.nodeLevel] || 'mid',
    desc: apiNode.diagnosisLabel || '',
    firms: '-',
    filtered: false,
    _raw: apiNode
  }
}

function transformLink(apiLink) {
  return { from: apiLink.fromNodeId, to: apiLink.toNodeId }
}

// ─── Reactive state ───────────────────────────────────────
const chainList = ref([])
const currentChain = ref(null)
const nodesRef = ref([])
const linksRef = ref([])
const layerFilter = ref('all')
const statusFilter = ref('all')
const searchQuery = ref('')
const panelOpen = ref(false)
const selectedNode = ref(null)
const chainContent = ref(null)
const nodeRefs = ref({})

function setNodeRef(id, el) {
  if (el) {
    nodeRefs.value[id] = el
  }
}

// ─── Computed ──────────────────────────────────────────────
const currentChainMeta = computed(() =>
  chainList.value.find(c => c.id === currentChain.value)
)

const chainData = computed(() => ({
  name: currentChainMeta.value?.chainName || '',
  total: currentChainMeta.value?.totalPatents ?? 0,
  nodes: nodesRef.value,
  links: linksRef.value
}))

const nodes = computed(() => nodesRef.value)

const chainTitle = computed(() =>
  (currentChainMeta.value?.chainName || '产业链') + '产业链全景图'
)

const chainMeta = computed(() => {
  const name = currentChainMeta.value?.chainName || ''
  const count = nodes.value.length
  const total = currentChainMeta.value?.totalPatents ?? 0
  return name + ' · ' + count + '个节点 · ' + total + '项专利'
})

const chainSubtitle = computed(() => {
  const up = nodes.value.filter(n => n.layer === 'up').length
  const mid = nodes.value.filter(n => n.layer === 'mid').length
  const down = nodes.value.filter(n => n.layer === 'down').length
  return up + '个上游 → ' + mid + '个中游 → ' + down + '个下游'
})

const statusCounts = computed(() => {
  const counts = { strong: 0, supplement: 0, weak: 0, missing: 0 }
  nodes.value.forEach(n => { if (counts[n.status] !== undefined) counts[n.status]++ })
  return counts
})

const nodeStatusColor = computed(() => {
  if (!selectedNode.value) return statusColors.strong
  return statusColors[selectedNode.value.status] || statusColors.missing
})

const highValueRatio = computed(() => {
  if (!selectedNode.value) return '—'
  if (selectedNode.value.status === 'missing') return '—'
  const hv = parseInt(selectedNode.value.highVal) || 0
  const p = parseInt(selectedNode.value.patents) || 1
  return Math.round(hv / Math.max(1, p) * 100) + '%'
})

const firmList = computed(() => {
  if (!selectedNode.value) return []
  const firms = selectedNode.value.firms || ''
  if (firms === '（技术空白）' || firms === '（新兴领域）' || firms === '-' || firms === '（暂无企业）') return []
  return firms.split('、').filter(Boolean)
})

// ─── SVG Links ────────────────────────────────────────────
function nodeSize(node) {
  const pat = parseInt(node.patents) || 1
  return Math.max(68, Math.min(100, 42 + Math.round(pat / 50)))
}

const visibleLinks = ref([])

function computeLinks() {
  const data = chainData.value
  if (!data || !data.nodes.length) { visibleLinks.value = []; return }

  const container = chainContent.value
  if (!container) { visibleLinks.value = []; return }

  const allNodes = data.nodes
  const nodeMap = {}
  for (const n of allNodes) {
    const el = nodeRefs.value[n.id]
    if (!el) continue
    const ring = el.querySelector('.node-ring')
    if (!ring) continue
    const ringRect = ring.getBoundingClientRect()
    const containerRect = container.getBoundingClientRect()
    nodeMap[n.id] = {
      cx: ringRect.left - containerRect.left + ringRect.width / 2,
      cy: ringRect.top - containerRect.top + ringRect.height / 2,
      right: ringRect.right - containerRect.left,
      left: ringRect.left - containerRect.left
    }
  }

  const links = []
  for (const lk of data.links) {
    const from = nodeMap[lk.from]
    const to = nodeMap[lk.to]
    if (!from || !to) continue

    const fromNode = nodes.value.find(n => n.id === lk.from)
    const toNode = nodes.value.find(n => n.id === lk.to)
    const bothActive = fromNode && toNode && !fromNode.filtered && !toNode.filtered

    const x1 = from.right, y1 = from.cy
    const x2 = to.left, y2 = to.cy
    const dx = Math.abs(x2 - x1)
    if (dx < 10) continue
    const cp = dx * 0.45
    links.push({
      d: 'M' + x1 + ',' + y1 + ' C' + (x1 + cp) + ',' + y1 + ' ' + (x2 - cp) + ',' + y2 + ' ' + x2 + ',' + y2,
      active: bothActive
    })
  }
  visibleLinks.value = links
}

function scheduleLinkDraw() {
  nextTick(() => {
    computeLinks()
    setTimeout(computeLinks, 300)
    setTimeout(computeLinks, 800)
  })
}

watch(currentChain, () => {
  scheduleLinkDraw()
})

// ─── Data loading ─────────────────────────────────────────
async function loadChainData(chainId) {
  try {
    const [apiNodes, apiLinks] = await Promise.all([
      getChainNodes(chainId),
      getChainLinks(chainId)
    ])
    nodesRef.value = (apiNodes || []).map(transformNode)
    linksRef.value = (apiLinks || []).map(transformLink)
  } catch (e) {
    console.error('Failed to load chain data', e)
    nodesRef.value = []
    linksRef.value = []
  }
}

// ─── Filters ───────────────────────────────────────────────
function applyFilters() {
  const layer = layerFilter.value
  const status = statusFilter.value
  const query = searchQuery.value.trim().toLowerCase()

  nodes.value.forEach(n => {
    const layerMatch = layer === 'all' || n.layer === layer
    const statusMatch = status === 'all' || n.status === status
    const searchMatch = !query || n.name.toLowerCase().includes(query)
    n.filtered = !(layerMatch && statusMatch && searchMatch)
  })

  nextTick(() => computeLinks())
}

function resetFilters() {
  layerFilter.value = 'all'
  statusFilter.value = 'all'
  searchQuery.value = ''
  nodes.value.forEach(n => { n.filtered = false })
  nextTick(() => computeLinks())
}

// ─── Chain switching ──────────────────────────────────────
async function switchChain(chainId) {
  currentChain.value = chainId
  selectedNode.value = null
  panelOpen.value = false
  resetFilters()
  await loadChainData(chainId)
  scheduleLinkDraw()
}

// ─── Node selection ───────────────────────────────────────
async function selectNode(id) {
  const node = nodes.value.find(n => n.id === id)
  if (!node) return
  selectedNode.value = { ...node }
  panelOpen.value = true

  // Fetch node detail for firm info
  try {
    const detail = await getNodeDetail(node._raw ? node._raw.id : id)
    const holders = detail.patentHolders || []
    selectedNode.value.firms = holders.length > 0
      ? holders.map(h => h.enterpriseName).join('、')
      : '（暂无企业）'
  } catch (e) {
    // firms remains as set by transformNode
  }
}

function closePanel() {
  panelOpen.value = false
  selectedNode.value = null
}

// ─── Navigation ───────────────────────────────────────────
function goToEnterprise(firm) {
  sessionStorage.setItem('navContext', JSON.stringify({ enterpriseName: firm }))
  router.push('/enterprise')
}

function goToDiagnosis() {
  if (selectedNode.value) {
    sessionStorage.setItem('navContext', JSON.stringify({ focusNode: selectedNode.value.name }))
  }
  closePanel()
  router.push('/diagnosis')
}

function goToInvestment() {
  if (selectedNode.value) {
    sessionStorage.setItem('navContext', JSON.stringify({ fromNode: selectedNode.value.name }))
  }
  closePanel()
  router.push('/investment')
}

// ─── Layers ────────────────────────────────────────────────
function getNodesByLayer(layer) {
  return nodes.value.filter(n => n.layer === layer)
}

// ─── Mount ─────────────────────────────────────────────────
onMounted(async () => {
  try {
    const chains = await getChainList()
    chainList.value = (chains || []).map(c => ({
      key: c.id,
      icon: getChainIcon(c.chainName),
      label: c.chainName,
      ...c
    }))
    if (chainList.value.length > 0) {
      currentChain.value = chainList.value[0].id
      await loadChainData(currentChain.value)
    }
  } catch (e) {
    console.error('Failed to load chain list', e)
  }
  scheduleLinkDraw()
  window.addEventListener('resize', computeLinks)
})

onUnmounted(() => {
  window.removeEventListener('resize', computeLinks)
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

.industry-tab {
  padding: 8px 18px;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  transition: all .2s;
  border: 1px solid #e2e8f0;
  background: #fff;
  color: #64748b;
  white-space: nowrap;
}
.industry-tab:hover { border-color: #93c5fd; color: #1e40af; background: #eff6ff; }
.industry-tab.active { background: #1e40af; color: #fff; border-color: #1e40af; box-shadow: 0 2px 8px rgba(30, 64, 175, .25); }

.graph-node { transition: all .25s; cursor: pointer; position: relative; z-index: 2; }
.graph-node:hover { z-index: 10; transform: scale(1.08); }

.select-field {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 13px;
  outline: none;
  background: #fff;
  cursor: pointer;
}

.input-field {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 13px;
  transition: all .2s;
  outline: none;
  background: #fff;
}
.input-field:focus { border-color: #3b82f6; box-shadow: 0 0 0 3px rgba(59, 130, 246, .15); }

.badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 500;
}

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

.scrollbar-hide::-webkit-scrollbar { display: none; }
.scrollbar-hide { -ms-overflow-style: none; scrollbar-width: none; }
</style>
