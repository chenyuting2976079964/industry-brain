<template>
  <div class="p-6 max-w-[1400px] mx-auto fade-in">
    <!-- ==================== HEADER ==================== -->
    <div class="flex items-center justify-between mb-5">
      <div>
        <h2 class="text-lg font-bold text-slate-800">
          <i class="fa-solid fa-bullseye mr-2 text-emerald-500"></i>精准招商 · 目标企业匹配
        </h2>
        <p class="text-xs text-slate-400 mt-0.5">{{ pageSubtitle }}</p>
      </div>
      <div class="flex gap-2">
        <span
          v-show="wishList.length > 0"
          class="text-xs text-slate-400 bg-white border border-slate-200 rounded-lg px-2.5 py-1.5"
        >
          <i class="fa-regular fa-bookmark mr-1 text-blue-500"></i>已选 <b>{{ wishList.length }}</b> 家
        </span>
        <button class="btn-outline text-xs" @click="handleExport">
          <i class="fa-regular fa-file-excel"></i>导出清单
        </button>
        <button class="btn-primary text-xs" @click="handleRematch">
          <i class="fa-solid fa-arrows-rotate"></i>重新匹配
        </button>
      </div>
    </div>

    <!-- ==================== SUB NAV TABS ==================== -->
    <div class="flex items-center gap-2 mb-4 bg-white rounded-xl p-1.5 border border-slate-100 w-fit">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        class="tab-btn"
        :class="{ active: activeTab === tab.key }"
        @click="switchTab(tab.key)"
      >
        <i :class="tab.icon + ' mr-1.5'"></i>{{ tab.label }}
      </button>
    </div>

    <!-- ==================== FILTER PANEL ==================== -->
    <div class="bg-white rounded-xl p-4 border border-slate-100 data-card mb-4">
      <div class="flex items-center gap-4 flex-wrap">
        <div>
          <label class="text-xs text-slate-400 block mb-1">薄弱环节</label>
          <select v-model="filters.weakness" class="select-field text-xs" @change="applyFilters">
            <option value="all">全部薄弱环节</option>
          </select>
        </div>
        <div>
          <label class="text-xs text-slate-400 block mb-1">匹配度</label>
          <select v-model="filters.minMatch" class="select-field text-xs" @change="applyFilters">
            <option value="all">全部</option>
            <option value="90">≥ 90%</option>
            <option value="80">≥ 80%</option>
            <option value="70">≥ 70%</option>
          </select>
        </div>
        <div>
          <label class="text-xs text-slate-400 block mb-1">目标规模</label>
          <select v-model="filters.scale" class="select-field text-xs" @change="applyFilters">
            <option value="all">全部规模</option>
            <option value="large">大型（1000人+）</option>
            <option value="mid">中型（200-1000人）</option>
            <option value="small">小型（200人以下）</option>
          </select>
        </div>
        <div>
          <label class="text-xs text-slate-400 block mb-1">排序</label>
          <select v-model="filters.sortBy" class="select-field text-xs" @change="applySort">
            <option value="match">匹配度优先</option>
            <option value="patent">专利实力优先</option>
            <option value="scale">产业带动优先</option>
          </select>
        </div>
        <div>
          <label class="text-xs text-slate-400 block mb-1">搜索企业</label>
          <div class="relative">
            <i class="fa-solid fa-search absolute left-2.5 top-1/2 -translate-y-1/2 text-slate-300 text-xs"></i>
            <input
              v-model="filters.keyword"
              class="input-field text-xs pl-7"
              placeholder="名称/技术领域..."
              @input="applyFilters"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- ==================== CONTEXT BANNER ==================== -->
    <div
      v-if="contextNode"
      class="bg-blue-50 border border-blue-200 rounded-xl p-3 mb-4 flex items-center justify-between"
    >
      <div class="flex items-center gap-2 text-xs text-blue-700">
        <i class="fa-solid fa-arrow-right"></i>来自诊断推荐：<span class="font-semibold">{{ contextNode }}</span> 环节的招商匹配结果
      </div>
      <button @click="contextNode = ''" class="text-blue-400 hover:text-blue-600">
        <i class="fa-solid fa-xmark"></i>
      </button>
    </div>

    <!-- ==================== ENTERPRISE CARDS ==================== -->
    <div class="space-y-3">
      <div
        v-for="ent in displayList"
        :key="ent.recommendationId"
        class="enterprise-card bg-white rounded-xl p-5 border border-slate-100"
      >
        <div class="flex items-start justify-between">
          <!-- Left: company info -->
          <div class="flex items-start gap-4 flex-1">
            <div
              class="w-12 h-12 rounded-xl flex items-center justify-center text-white font-bold text-lg shadow-lg flex-shrink-0"
              :style="{ background: gradientFor(ent.initial) }"
            >
              {{ ent.initial }}
            </div>
            <div class="flex-1">
              <div class="flex items-center gap-2 flex-wrap">
                <h3 class="font-semibold text-slate-800">{{ ent.enterpriseName }}</h3>
                <span
                  v-for="(tag, i) in ent.tags"
                  :key="i"
                  class="badge text-xs"
                  :class="ent.tagStyles[i]"
                >{{ tag }}</span>
              </div>
              <p class="text-xs text-slate-400 mt-1">{{ ent.description || ent.location || '' }}</p>
              <div class="flex gap-3 mt-1.5 text-xs text-slate-500">
                <span><i class="fa-solid fa-sitemap mr-1"></i>{{ ent.nodeName || '未知节点' }}</span>
                <span><i class="fa-regular fa-circle-dot mr-1"></i>{{ ent.diagnosisTag || '' }}</span>
              </div>
            </div>
          </div>
          <!-- Right: match score circle -->
          <div class="text-right flex-shrink-0">
            <div class="w-16 h-16 rounded-full bg-emerald-50 border-4 border-emerald-200 inline-flex items-center justify-center">
              <span class="text-emerald-600 font-bold text-lg">{{ ent.matchScore }}</span>
            </div>
            <p class="text-[10px] text-emerald-600 mt-1">匹配度</p>
          </div>
        </div>

        <!-- Match Reasons -->
        <div class="mt-3 pt-3 border-t border-slate-100" v-if="ent.matchReasons && ent.matchReasons.length">
          <div class="flex items-center gap-1 text-xs text-slate-400 mb-2 flex-wrap">
            <span class="font-medium text-slate-600">匹配理由：</span>
            <template v-for="(reason, ri) in ent.matchReasons" :key="ri">
              <span class="px-1.5 py-0.5 rounded bg-blue-50 text-blue-600">{{ reason }}</span>
              <span v-if="ri < ent.matchReasons.length - 1" class="text-slate-300">→</span>
            </template>
          </div>
        </div>

        <!-- Metric Bars -->
        <div class="mt-3 pt-3 border-t border-slate-100">
          <div class="grid grid-cols-3 gap-3">
            <div class="bg-slate-50 rounded-lg p-2.5">
              <p class="text-xs text-slate-400">技术匹配度</p>
              <div class="flex items-center gap-2 mt-0.5">
                <div class="flex-1 h-1.5 bg-slate-200 rounded-full overflow-hidden">
                  <div class="h-full bg-emerald-500 rounded-full" :style="{ width: (ent.matchScore || 0) + '%' }"></div>
                </div>
                <span class="text-xs font-medium">{{ ent.matchScore || 0 }}%</span>
              </div>
            </div>
            <div class="bg-slate-50 rounded-lg p-2.5">
              <p class="text-xs text-slate-400">专利实力</p>
              <div class="flex items-center gap-2 mt-0.5">
                <div class="flex-1 h-1.5 bg-slate-200 rounded-full overflow-hidden">
                  <div class="h-full bg-blue-500 rounded-full" :style="{ width: (ent.patentStrengthScore || 0) + '%' }"></div>
                </div>
                <span class="text-xs font-medium">{{ ent.patentStrengthScore || 0 }}%</span>
              </div>
            </div>
            <div class="bg-slate-50 rounded-lg p-2.5">
              <p class="text-xs text-slate-400">产业带动潜力</p>
              <div class="flex items-center gap-2 mt-0.5">
                <div class="flex-1 h-1.5 bg-slate-200 rounded-full overflow-hidden">
                  <div class="h-full bg-cyan-500 rounded-full" :style="{ width: (ent.drivingPotentialScore || 0) + '%' }"></div>
                </div>
                <span class="text-xs font-medium">{{ ent.drivingPotentialScore || 0 }}%</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Actions -->
        <div class="flex justify-end gap-2 mt-3 pt-3 border-t border-slate-100">
          <button
            class="btn-outline text-xs"
            :class="{ 'btn-primary': wishList.includes(ent.id) }"
            @click="toggleWishlist(ent.id)"
          >
            <i :class="wishList.includes(ent.id) ? 'fa-solid fa-bookmark' : 'fa-regular fa-bookmark'"></i>
            {{ wishList.includes(ent.id) ? '已加入' : '加入清单' }}
          </button>
          <button class="btn-primary text-xs" @click="openDetail(ent)">
            <i class="fa-regular fa-eye"></i> 查看企业
          </button>
        </div>
      </div>
    </div>

    <!-- ==================== PAGINATION ==================== -->
    <div class="flex items-center justify-between mt-5">
      <p class="text-xs text-slate-400">共 {{ totalCount }} 家匹配企业，当前显示 {{ displayList.length }} 家</p>
      <div class="flex items-center gap-1">
        <button class="w-8 h-8 rounded-lg bg-white border border-slate-200 text-xs text-slate-500 flex items-center justify-center hover:bg-slate-50">
          <i class="fa-solid fa-chevron-left text-[10px]"></i>
        </button>
        <button class="w-8 h-8 rounded-lg bg-blue-600 text-white text-xs font-medium flex items-center justify-center">1</button>
        <button class="w-8 h-8 rounded-lg bg-white border border-slate-200 text-xs text-slate-500 flex items-center justify-center hover:bg-slate-50">
          <i class="fa-solid fa-chevron-right text-[10px]"></i>
        </button>
      </div>
    </div>

    <!-- ==================== SLIDE PANEL: Enterprise Detail ==================== -->
    <Teleport to="body">
      <div v-if="panelVisible" class="panel-overlay show" @click="closeDetail"></div>
      <div class="slide-panel" :class="{ open: panelVisible }">
        <div v-if="selectedEnt" class="slide-panel-inner">
          <!-- Panel Header -->
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-lg font-bold text-slate-800">
              <i class="fa-regular fa-building mr-2 text-blue-600"></i>企业详情
            </h3>
            <button @click="closeDetail" class="w-8 h-8 rounded-lg hover:bg-slate-100 flex items-center justify-center text-slate-400">
              <i class="fa-solid fa-xmark"></i>
            </button>
          </div>

          <!-- Company Header -->
          <div class="flex items-start gap-3 mb-4">
            <div
              class="w-12 h-12 rounded-xl flex items-center justify-center text-white text-lg font-bold flex-shrink-0"
              :style="{ background: gradientFor(selectedEnt.initial) }"
            >
              {{ selectedEnt.initial }}
            </div>
            <div>
              <p class="text-base font-bold text-slate-800">{{ selectedEnt.enterpriseName }}</p>
              <p class="text-xs text-slate-400">{{ selectedEnt.description || selectedEnt.location || '' }}</p>
              <div class="flex flex-wrap gap-2 mt-1">
                <span
                  v-for="(tag, i) in selectedEnt.tags"
                  :key="i"
                  class="badge"
                  :class="selectedEnt.tagStyles[i]"
                >{{ tag }}</span>
              </div>
            </div>
          </div>

          <!-- Meta Info -->
          <div class="flex flex-wrap gap-4 text-xs text-slate-500 mb-4 pb-3 border-b border-slate-100">
            <span><i class="fa-solid fa-location-dot mr-1"></i>{{ selectedEnt.location || '--' }}</span>
            <span><i class="fa-solid fa-sitemap mr-1"></i>{{ selectedEnt.nodeName || '--' }}</span>
            <span><i class="fa-solid fa-tag mr-1"></i>{{ selectedEnt.diagnosisTag || '--' }}</span>
          </div>

          <!-- Stats Grid -->
          <div class="grid grid-cols-4 gap-2 mb-4">
            <div class="bg-slate-50 rounded-lg p-2.5 text-center">
              <p class="text-xl font-bold text-slate-800">{{ selectedEnt.patentCount }}</p>
              <p class="text-xs text-slate-400">专利总数</p>
            </div>
            <div class="bg-slate-50 rounded-lg p-2.5 text-center">
              <p class="text-xl font-bold text-emerald-600">{{ selectedEnt.highValueCount }}</p>
              <p class="text-xs text-slate-400">高价值</p>
            </div>
            <div class="bg-slate-50 rounded-lg p-2.5 text-center">
              <p class="text-xl font-bold text-blue-600">{{ selectedEnt.inventionCount }}</p>
              <p class="text-xs text-slate-400">发明专利</p>
            </div>
            <div class="bg-slate-50 rounded-lg p-2.5 text-center">
              <p class="text-xl font-bold text-cyan-600">{{ selectedEnt.techMatchRate }}</p>
              <p class="text-xs text-slate-400">匹配度</p>
            </div>
          </div>

          <!-- Tech Advantages -->
          <h4 class="text-sm font-semibold text-slate-700 mb-2">技术优势</h4>
          <div class="space-y-1.5 mb-3">
            <div v-for="(adv, i) in selectedEnt.advantages" :key="i">
              <div class="flex justify-between text-xs">
                <span>{{ adv.name }}</span>
                <span class="text-slate-400">{{ adv.count }}</span>
              </div>
              <div class="h-2 bg-blue-100 rounded-full overflow-hidden">
                <div
                  class="h-full bg-blue-600 rounded-full transition-all duration-500"
                  :style="{ width: [92, 82, 58][i] + '%' }"
                ></div>
              </div>
            </div>
          </div>

          <!-- R&D Tags -->
          <h4 class="text-sm font-semibold text-slate-700 mb-2">研发方向</h4>
          <div class="flex flex-wrap gap-1.5 mb-3">
            <span
              v-for="tag in selectedEnt.techTags"
              :key="tag"
              class="badge bg-blue-50 text-blue-600 text-xs"
            >{{ tag }}</span>
          </div>

          <!-- Weakness -->
          <div class="bg-red-50 rounded-lg p-3 border border-red-100">
            <div class="flex items-start gap-2">
              <i class="fa-solid fa-triangle-exclamation text-red-400 mt-0.5 text-xs"></i>
              <div>
                <p class="text-xs font-medium text-slate-700">{{ selectedEnt.weaknessTitle }}</p>
                <p class="text-xs text-slate-400 mt-0.5">{{ selectedEnt.weaknessDetail }}</p>
              </div>
            </div>
          </div>

          <!-- Investment Score -->
          <div class="mt-4 pt-3 border-t border-slate-100">
            <h4 class="text-sm font-semibold text-slate-700 mb-2">招商价值评分</h4>
            <div class="text-center mb-3">
              <div
                class="inline-flex items-center justify-center w-16 h-16 rounded-full border-4"
                :class="selectedEnt.evalLevel === 'A' ? 'bg-emerald-50 border-emerald-300' : 'bg-amber-50 border-amber-300'"
              >
                <span
                  class="font-bold text-2xl"
                  :class="selectedEnt.evalLevel === 'A' ? 'text-emerald-600' : 'text-amber-600'"
                >{{ selectedEnt.evalLevel }}</span>
              </div>
              <p
                class="text-xs font-medium mt-1"
                :class="selectedEnt.evalLevel === 'A' ? 'text-emerald-600' : 'text-amber-600'"
              >{{ selectedEnt.evalLabel }}</p>
            </div>
            <div class="space-y-1.5">
              <div v-for="score in selectedEnt.scores" :key="score.name">
                <div class="flex justify-between text-xs mb-0.5">
                  <span>{{ score.name }}</span>
                  <span>{{ score.value }}%</span>
                </div>
                <div class="h-1.5 bg-slate-100 rounded-full overflow-hidden">
                  <div
                    class="h-full rounded-full transition-all duration-500"
                    :class="'bg-' + score.color + '-500'"
                    :style="{ width: score.value + '%' }"
                  ></div>
                </div>
              </div>
            </div>
          </div>

          <!-- Patent List (if available) -->
          <div v-if="selectedEnt.patentList && selectedEnt.patentList.length > 0" class="mt-4 pt-3 border-t border-slate-100">
            <h4 class="text-sm font-semibold text-slate-700 mb-2">代表性专利</h4>
            <div class="space-y-2">
              <div
                v-for="(pt, i) in selectedEnt.patentList"
                :key="i"
                class="flex items-center gap-3 p-3 bg-slate-50 rounded-lg"
              >
                <div class="w-7 h-7 rounded-full flex items-center justify-center flex-shrink-0 text-xs bg-blue-100">
                  <i :class="['fa-solid', rankIcons[i] || 'fa-file', rankColors[i] || 'text-slate-400'].join(' ') + ' text-xs'"></i>
                </div>
                <div class="flex-1 min-w-0">
                  <p class="text-xs font-medium text-slate-700">{{ pt.name }}</p>
                  <div class="flex items-center gap-2 mt-0.5">
                    <span class="text-[10px] text-blue-500 bg-blue-50 px-1.5 py-0.5 rounded">{{ pt.type }}</span>
                    <span class="text-[10px] text-slate-400">{{ pt.date }}</span>
                    <span class="text-[10px] text-emerald-500">{{ pt.status }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useEventBus } from '@/stores/eventBus'
import { investmentService } from '@/services/investment.service'

const eventBus = useEventBus()

// ---- State ----
const activeTab = ref('all')
const pageSubtitle = ref('基于弱链/缺链节点自动匹配 · 综合评估招商价值')
const contextNode = ref('')
const panelVisible = ref(false)
const selectedEnt = ref(null)
const displayList = ref([])
const totalCount = ref(0)
const wishList = ref([])
const rematching = ref(false)

const tabs = [
  { key: 'all', label: '全部推荐', icon: 'fa-solid fa-list' }
]

const filters = reactive({
  weakness: 'all',
  minMatch: 'all',
  scale: 'all',
  sortBy: 'match',
  keyword: ''
})

const rankIcons = ['fa-crown', 'fa-2', 'fa-3', 'fa-4', 'fa-5']
const rankColors = ['text-amber-500', 'text-slate-400', 'text-orange-700', 'text-slate-400', 'text-slate-400']

const GRADIENTS = [
  'linear-gradient(135deg, #667eea, #764ba2)',
  'linear-gradient(135deg, #f093fb, #f5576c)',
  'linear-gradient(135deg, #4facfe, #00f2fe)',
  'linear-gradient(135deg, #43e97b, #38f9d7)',
  'linear-gradient(135deg, #fa709a, #fee140)',
  'linear-gradient(135deg, #a18cd1, #fbc2eb)',
  'linear-gradient(135deg, #fccb90, #d57eeb)',
  'linear-gradient(135deg, #e0c3fc, #8ec5fc)',
]
function gradientFor(initial) {
  const code = (initial || '?').charCodeAt(0)
  return GRADIENTS[code % GRADIENTS.length]
}

// ---- Init ----
onMounted(async () => {
  // Read context from navigation (e.g. from diagnosis alert)
  try {
    const ctx = JSON.parse(sessionStorage.getItem('navContext') || '{}')
    if (ctx.fromNode || ctx.weakNode) {
      const node = ctx.fromNode || ctx.weakNode
      contextNode.value = node
      pageSubtitle.value = `来自 ${node} 环节的招商匹配`
      if (ctx.weaknessKey) {
        filters.weakness = ctx.weaknessKey
      }
    }
    if (ctx.tab) {
      activeTab.value = ctx.tab
    }
    // Clear context after reading
    sessionStorage.removeItem('navContext')
  } catch (e) { console.error('[Investment] Failed to parse nav context:', e) }

  await loadData()
})

// ---- Data loading ----
async function loadData() {
  try {
    const res = await investmentService.getEnterprises({
      weakness: filters.weakness,
      minMatch: filters.minMatch,
      scale: filters.scale,
      sortBy: filters.sortBy,
      keyword: filters.keyword
    })
    totalCount.value = res.total
    displayList.value = applyTabFilter(res.list)
  } catch (e) {
    console.error('Failed to load investment data:', e)
  }
}

function applyTabFilter(list) {
  if (activeTab.value === 'high') {
    return list.filter(e => e.matchScore >= 85)
  }
  if (activeTab.value === 'weakchain') {
    return list.filter(e => e.weakness && ['precision', 'sensor', 'ai', 'metal'].includes(e.weakness))
  }
  return list
}

// ---- Actions ----
async function switchTab(key) {
  activeTab.value = key
  await loadData()
}

async function applyFilters() {
  await loadData()
}

function applySort() {
  const list = [...displayList.value]
  if (filters.sortBy === 'patent') {
    list.sort((a, b) => b.patentScore - a.patentScore)
  } else if (filters.sortBy === 'scale') {
    list.sort((a, b) => b.driveScore - a.driveScore)
  } else {
    list.sort((a, b) => b.matchScore - a.matchScore)
  }
  displayList.value = list
}

function openDetail(ent) {
  selectedEnt.value = ent
  panelVisible.value = true
}

function closeDetail() {
  panelVisible.value = false
  setTimeout(() => { selectedEnt.value = null }, 300)
}

function toggleWishlist(id) {
  const idx = wishList.value.indexOf(id)
  if (idx > -1) {
    wishList.value.splice(idx, 1)
  } else {
    wishList.value.push(id)
  }
}

async function handleExport() {
  await investmentService.exportList('excel')
  eventBus.openModal('导出完成', `
    <div class="text-center py-4">
      <i class="fa-solid fa-circle-check text-emerald-500 text-4xl mb-3"></i>
      <p class="text-sm text-slate-600">招商清单已导出</p>
      <p class="text-xs text-slate-400 mt-1">文件已保存至下载目录</p>
    </div>
  `)
}

async function handleRematch() {
  rematching.value = true
  try {
    await investmentService.rematch()
    eventBus.openModal('匹配完成', `
      <div class="text-center py-4">
        <i class="fa-solid fa-circle-check text-emerald-500 text-4xl mb-3"></i>
        <p class="text-sm text-slate-600">重新匹配完成，已更新推荐结果</p>
      </div>
    `)
    await loadData()
  } catch (e) {
    console.error('[Investment] Rematch failed:', e)
  } finally {
    rematching.value = false
  }
}
</script>

<style scoped>
.enterprise-card {
  transition: all 0.2s;
}
.enterprise-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.08);
}

.slide-panel {
  position: fixed;
  top: 0;
  right: -620px;
  width: 620px;
  height: 100%;
  background: #fff;
  z-index: 100;
  box-shadow: -4px 0 30px rgba(0, 0, 0, 0.12);
  transition: right 0.3s ease;
}
.slide-panel.open {
  right: 0;
}
.slide-panel-inner {
  height: 100%;
  overflow-y: auto;
  padding: 24px;
}

.panel-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 99;
  display: none;
}
.panel-overlay.show {
  display: block;
}

/* ========== Tab Button ========== */
.tab-btn {
  padding: 8px 20px;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
  background: transparent;
  color: #64748b;
}
.tab-btn.active {
  background: #1e40af;
  color: #fff;
  box-shadow: 0 2px 8px rgba(30, 64, 175, 0.3);
}
.tab-btn:hover:not(.active) {
  background: #e2e8f0;
}

/* ========== Shared component classes (also defined globally) ========== */
.btn-primary {
  background: #1e40af;
  color: #fff;
  padding: 8px 20px;
  border-radius: 8px;
  font-size: 13px;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.btn-primary:hover {
  background: #1e3a8a;
  box-shadow: 0 4px 12px rgba(30, 64, 175, 0.3);
}

.btn-outline {
  border: 1px solid #e2e8f0;
  background: #fff;
  color: #1e293b;
  padding: 8px 20px;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.btn-outline:hover {
  border-color: #1e40af;
  color: #1e40af;
}

.input-field {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 13px;
  transition: all 0.2s;
  outline: none;
  background: #fff;
}
.input-field:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15);
}

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

.badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 500;
}
</style>
