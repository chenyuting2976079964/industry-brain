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
        <button class="btn-outline text-xs" @click="handleExport" :disabled="exporting">
          <i :class="exporting ? 'fa-solid fa-spinner fa-spin' : 'fa-regular fa-file-excel'"></i>{{ exporting ? '导出中...' : '导出清单' }}
        </button>
        <button class="btn-primary text-xs" @click="handleRematch" :disabled="rematching">
          <i :class="rematching ? 'fa-solid fa-spinner fa-spin' : 'fa-solid fa-arrows-rotate'"></i>{{ rematching ? '匹配中...' : '重新匹配' }}
        </button>
      </div>
    </div>

    <!-- ==================== FILTER PANEL ==================== -->
    <div class="bg-white rounded-xl p-4 border border-slate-100 data-card mb-4">
      <div class="flex items-center gap-4 flex-wrap">
        <div>
          <label class="text-xs text-slate-400 block mb-1">薄弱环节</label>
          <select v-model="filters.weakness" class="select-field text-xs" @change="applyFilters">
            <option value="all">全部薄弱环节</option>
            <option v-for="w in weaknessOptions" :key="w.nodeName" :value="w.diagnosisTag">{{ w.nodeName }}（{{ w.diagnosisTag }}）</option>
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
            <option value="大型">大型</option>
            <option value="中大型">中大型</option>
            <option value="中型">中型</option>
            <option value="中小型">中小型</option>
            <option value="小型">小型</option>
          </select>
        </div>
        <div>
          <label class="text-xs text-slate-400 block mb-1">排序</label>
          <select v-model="filters.sortBy" class="select-field text-xs" @change="applySort">
            <option value="match">综合得分优先</option>
            <option value="business">业务契合度优先</option>
            <option value="scale">规模实力优先</option>
            <option value="region">地域协同度优先</option>
          </select>
        </div>
        <div>
          <label class="text-xs text-slate-400 block mb-1">搜索企业</label>
          <div class="flex items-center border border-slate-200 rounded-lg bg-white overflow-hidden focus-within:border-blue-400 focus-within:shadow-[0_0_0_3px_rgba(59,130,246,0.15)] transition-all">
            <span class="pl-3 pr-1.5 text-slate-300 text-xs"><i class="fa-solid fa-search"></i></span>
            <input
              v-model="filters.keyword"
              class="flex-1 py-2 pr-3 text-xs border-none outline-none bg-transparent"
              placeholder="企业名称..."
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
              class="w-12 h-12 bg-gradient-to-br from-blue-500 to-indigo-600 rounded-xl flex items-center justify-center text-white font-bold text-lg shadow-lg flex-shrink-0"
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
              <p class="text-xs text-slate-400 mt-1" :class="{ 'line-clamp-2': !ent.descExpanded }">{{ ent.description || '暂无描述' }}</p>
              <button
                v-if="ent.description && ent.description.length > 60"
                class="text-xs text-blue-500 hover:text-blue-600 mt-0.5"
                @click="ent.descExpanded = !ent.descExpanded"
              >{{ ent.descExpanded ? '收起' : '展开' }}</button>
              <div class="flex gap-3 mt-1.5 text-xs text-slate-500">
                <span v-if="ent.location"><i class="fa-solid fa-location-dot mr-1"></i>{{ ent.location }}</span>
                <span v-if="ent.scale"><i class="fa-regular fa-user mr-1"></i>{{ ent.scale }}</span>
                <span v-if="ent.registeredCapital"><i class="fa-regular fa-building mr-1"></i>{{ ent.registeredCapital }}</span>
              </div>
            </div>
          </div>
          <!-- Right: match score circle -->
          <div class="text-right flex-shrink-0">
            <div class="w-16 h-16 rounded-full bg-emerald-50 border-4 border-emerald-200 inline-flex items-center justify-center">
              <span class="text-emerald-600 font-bold text-lg">{{ ent.matchScore }}</span>
            </div>
            <p class="text-[10px] text-emerald-600 mt-1">综合</p>
          </div>
        </div>

        <!-- Match Reasons -->
        <div class="mt-3 pt-3 border-t border-slate-100">
          <div class="flex items-center gap-1 text-xs text-slate-400 mb-2 flex-wrap" v-if="ent.matchReasons && ent.matchReasons.length">
            <span class="font-medium text-slate-600">匹配理由：</span>
            <template v-for="(reason, ri) in ent.matchReasons" :key="ri">
              <span class="px-1.5 py-0.5 rounded bg-slate-100 text-slate-600">{{ reason }}</span>
              <span v-if="ri < ent.matchReasons.length - 1" class="text-slate-300">·</span>
            </template>
          </div>

          <!-- Metric Bars -->
          <div class="grid grid-cols-3 gap-3">
            <div class="bg-slate-50 rounded-lg p-2.5">
              <p class="text-xs text-slate-400">业务契合度</p>
              <div class="flex items-center gap-2 mt-0.5">
                <div class="flex-1 h-1.5 bg-slate-200 rounded-full overflow-hidden">
                  <div class="h-full bg-emerald-500 rounded-full transition-all duration-500" :style="{ width: ent.businessFitScore + '%' }"></div>
                </div>
                <span class="text-xs font-medium">{{ ent.businessFitScore }}%</span>
              </div>
            </div>
            <div class="bg-slate-50 rounded-lg p-2.5">
              <p class="text-xs text-slate-400">规模实力</p>
              <div class="flex items-center gap-2 mt-0.5">
                <div class="flex-1 h-1.5 bg-slate-200 rounded-full overflow-hidden">
                  <div class="h-full bg-blue-500 rounded-full transition-all duration-500" :style="{ width: ent.scaleStrengthScore + '%' }"></div>
                </div>
                <span class="text-xs font-medium">{{ ent.scaleStrengthScore }}%</span>
              </div>
            </div>
            <div class="bg-slate-50 rounded-lg p-2.5">
              <p class="text-xs text-slate-400">地域协同度</p>
              <div class="flex items-center gap-2 mt-0.5">
                <div class="flex-1 h-1.5 bg-slate-200 rounded-full overflow-hidden">
                  <div class="h-full bg-cyan-500 rounded-full transition-all duration-500" :style="{ width: ent.regionalSynergyScore + '%' }"></div>
                </div>
                <span class="text-xs font-medium">{{ ent.regionalSynergyScore }}%</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Actions -->
        <div class="flex justify-end gap-2 mt-3 pt-3 border-t border-slate-100">
          <button
            class="btn-outline text-xs"
            :class="{ 'btn-primary': ent.inWishlist }"
            @click="toggleWishlist(ent)"
          >
            <i :class="ent.inWishlist ? 'fa-solid fa-bookmark' : 'fa-regular fa-bookmark'"></i>
            {{ ent.inWishlist ? '已加入' : '加入清单' }}
          </button>
          <button class="btn-primary text-xs" @click="openDetail(ent)">
            <i class="fa-regular fa-eye"></i> 查看企业
          </button>
        </div>
      </div>
    </div>

    <!-- ==================== PAGINATION ==================== -->
    <div class="flex items-center justify-between mt-5">
      <div class="flex items-center gap-4">
        <p class="text-xs text-slate-400">共 {{ totalCount }} 家匹配企业</p>
        <select v-model="pageSize" class="select-field text-xs w-auto" @change="goToPage(1)">
          <option :value="10">10条/页</option>
          <option :value="20">20条/页</option>
          <option :value="50">50条/页</option>
        </select>
      </div>
      <div class="flex items-center gap-1">
        <button
          class="w-8 h-8 rounded-lg bg-white border border-slate-200 text-xs text-slate-500 flex items-center justify-center hover:bg-slate-50"
          :disabled="currentPage <= 1"
          @click="goToPage(currentPage - 1)"
        >
          <i class="fa-solid fa-chevron-left text-[10px]"></i>
        </button>
        <button
          v-for="p in visiblePages"
          :key="p"
          :class="['w-8 h-8 rounded-lg text-xs font-medium flex items-center justify-center', p === currentPage ? 'bg-blue-600 text-white' : 'bg-white border border-slate-200 text-slate-500 hover:bg-slate-50']"
          @click="goToPage(p)"
        >{{ p }}</button>
        <button
          class="w-8 h-8 rounded-lg bg-white border border-slate-200 text-xs text-slate-500 flex items-center justify-center hover:bg-slate-50"
          :disabled="currentPage >= totalPages"
          @click="goToPage(currentPage + 1)"
        >
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
            <div class="w-12 h-12 bg-gradient-to-br from-blue-500 to-indigo-600 rounded-xl flex items-center justify-center text-white text-lg font-bold flex-shrink-0">
              {{ selectedEnt.initial }}
            </div>
            <div>
              <p class="text-base font-bold text-slate-800">{{ selectedEnt.enterpriseName }}</p>
              <p class="text-xs text-slate-400 mt-1">{{ selectedEnt.nodeName }} · {{ selectedEnt.diagnosisTag }}</p>
              <div class="flex flex-wrap gap-2 mt-1">
                <span v-for="(tag, i) in selectedEnt.tags" :key="i" class="badge" :class="selectedEnt.tagStyles[i]">{{ tag }}</span>
              </div>
            </div>
          </div>

          <!-- Meta Info -->
          <div class="flex flex-wrap gap-4 text-xs text-slate-500 mb-4 pb-3 border-b border-slate-100">
            <span v-if="selectedEnt.location"><i class="fa-solid fa-location-dot mr-1"></i>{{ selectedEnt.location }}</span>
            <span v-if="selectedEnt.scale"><i class="fa-regular fa-user mr-1"></i>{{ selectedEnt.scale }}</span>
            <span v-if="selectedEnt.registeredCapital"><i class="fa-regular fa-building mr-1"></i>{{ selectedEnt.registeredCapital }}</span>
            <span><i class="fa-solid fa-hashtag mr-1"></i>{{ selectedEnt.rating }} 级</span>
          </div>

          <!-- Three-dimension scores -->
          <h4 class="text-sm font-semibold text-slate-700 mb-3">三维度招商评分</h4>
          <div class="space-y-3 mb-4">
            <div>
              <div class="flex justify-between text-xs mb-1"><span>业务契合度</span><span class="font-medium">{{ selectedEnt.businessFitScore }}%</span></div>
              <div class="h-2 bg-slate-100 rounded-full overflow-hidden"><div class="h-full bg-emerald-500 rounded-full" :style="{ width: (selectedEnt.businessFitScore || 0) + '%' }"></div></div>
            </div>
            <div>
              <div class="flex justify-between text-xs mb-1"><span>规模实力</span><span class="font-medium">{{ selectedEnt.scaleStrengthScore }}%</span></div>
              <div class="h-2 bg-slate-100 rounded-full overflow-hidden"><div class="h-full bg-blue-500 rounded-full" :style="{ width: (selectedEnt.scaleStrengthScore || 0) + '%' }"></div></div>
            </div>
            <div>
              <div class="flex justify-between text-xs mb-1"><span>地域协同度</span><span class="font-medium">{{ selectedEnt.regionalSynergyScore }}%</span></div>
              <div class="h-2 bg-slate-100 rounded-full overflow-hidden"><div class="h-full bg-cyan-500 rounded-full" :style="{ width: (selectedEnt.regionalSynergyScore || 0) + '%' }"></div></div>
            </div>
          </div>

          <!-- Overall Score -->
          <div class="bg-slate-50 rounded-xl p-4 mb-4 text-center">
            <div class="inline-flex items-center justify-center w-20 h-20 rounded-full border-4" :class="selectedEnt.rating === 'A' ? 'bg-emerald-50 border-emerald-300' : selectedEnt.rating === 'B' ? 'bg-blue-50 border-blue-300' : 'bg-slate-100 border-slate-300'">
              <span class="font-bold text-2xl" :class="selectedEnt.rating === 'A' ? 'text-emerald-600' : selectedEnt.rating === 'B' ? 'text-blue-600' : 'text-slate-500'">{{ selectedEnt.totalScore }}</span>
            </div>
            <p class="text-sm font-semibold mt-2 text-slate-700">综合得分 {{ selectedEnt.rating }} 级</p>
            <p class="text-xs text-slate-400 mt-1">{{ selectedEnt.matchLevelText }}</p>
          </div>

          <!-- Match Reasons -->
          <div v-if="selectedEnt.matchReasons && selectedEnt.matchReasons.length" class="mb-4">
            <h4 class="text-sm font-semibold text-slate-700 mb-2">匹配理由</h4>
            <div class="space-y-1">
              <p v-for="(r, i) in selectedEnt.matchReasons" :key="i" class="text-xs text-slate-500 bg-slate-50 rounded-lg px-3 py-1.5">{{ r }}</p>
            </div>
          </div>

          <!-- Description -->
          <div class="mb-4">
            <h4 class="text-sm font-semibold text-slate-700 mb-2">经营范围</h4>
            <p class="text-xs text-slate-500 leading-relaxed bg-slate-50 rounded-lg p-3" :class="{ 'line-clamp-6': !selectedEnt.descExpanded }">{{ selectedEnt.description || '暂无' }}</p>
            <button v-if="selectedEnt.description && selectedEnt.description.length > 200" class="text-xs text-blue-500 hover:text-blue-600 mt-1" @click="selectedEnt.descExpanded = !selectedEnt.descExpanded">{{ selectedEnt.descExpanded ? '收起' : '展开全文' }}</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { investmentService } from '@/services/investment.service'
import { reportsService } from '@/services/reports.service'
import { getDiagnosisList } from '@/services/diagnosis.service'

// ---- State ----
const pageSubtitle = ref('基于弱链/缺链节点自动匹配 · 综合评估招商价值')
const contextNode = ref('')
const panelVisible = ref(false)
const selectedEnt = ref(null)
const displayList = ref([])
const totalCount = ref(0)
const wishList = ref([])
const rematching = ref(false)
const exporting = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const filters = reactive({
  weakness: 'all',
  minMatch: 'all',
  scale: 'all',
  sortBy: 'match',
  keyword: ''
})

// ---- Pagination ----
const totalPages = computed(() => Math.max(1, Math.ceil(totalCount.value / pageSize.value)))

const visiblePages = computed(() => {
  const total = totalPages.value
  const current = currentPage.value
  const pages = []
  let start = Math.max(1, current - 2)
  let end = Math.min(total, current + 2)
  if (end - start < 4) {
    if (start === 1) end = Math.min(total, start + 4)
    else start = Math.max(1, end - 4)
  }
  for (let i = start; i <= end; i++) pages.push(i)
  return pages
})

// 薄弱环节选项（从数据中动态提取）
const weaknessOptions = ref([])

// ---- Init ----
onMounted(async () => {
  try {
    const ctx = JSON.parse(sessionStorage.getItem('navContext') || '{}')
    if (ctx.fromNode || ctx.weakNode) {
      contextNode.value = ctx.fromNode || ctx.weakNode
      pageSubtitle.value = `来自 ${contextNode.value} 环节的招商匹配`
      if (ctx.weaknessKey) filters.weakness = ctx.weaknessKey
    }
    sessionStorage.removeItem('navContext')
  } catch (e) { console.error('[Investment] Failed to parse nav context:', e) }

  await Promise.all([loadWeaknessOptions(), loadData()])
})

// ---- Data loading ----
async function loadData() {
  try {
    const res = await investmentService.getEnterprises({
      weakness: filters.weakness,
      minMatch: filters.minMatch,
      scale: filters.scale,
      sortBy: filters.sortBy,
      keyword: filters.keyword,
      page: currentPage.value,
      size: pageSize.value
    })
    totalCount.value = res.total
    displayList.value = res.enterprises || []
  } catch (e) {
    console.error('Failed to load investment data:', e)
  }
}

function goToPage(page) {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
  loadData()
}

// 从诊断 API 加载全部薄弱环节节点（缺链+弱链+补链）
async function loadWeaknessOptions() {
  try {
    const allResults = await getDiagnosisList()
    const tags = ['缺链', '弱链', '补链']
    weaknessOptions.value = (allResults || [])
      .filter(item => tags.includes(item.diagnosisTag))
      .map(item => ({ nodeName: item.nodeName, diagnosisTag: item.diagnosisTag }))
  } catch (e) {
    console.error('Failed to load weakness options:', e)
  }
}

// ---- Actions ----
async function applyFilters() { currentPage.value = 1; await loadData() }

function applySort() {
  const list = [...displayList.value]
  if (filters.sortBy === 'business') list.sort((a, b) => (b.businessFitScore || 0) - (a.businessFitScore || 0))
  else if (filters.sortBy === 'scale') list.sort((a, b) => (b.scaleStrengthScore || 0) - (a.scaleStrengthScore || 0))
  else if (filters.sortBy === 'region') list.sort((a, b) => (b.regionalSynergyScore || 0) - (a.regionalSynergyScore || 0))
  else list.sort((a, b) => (b.matchScore || 0) - (a.matchScore || 0))
  displayList.value = list
}

function openDetail(ent) {
  selectedEnt.value = { ...ent, descExpanded: false }
  panelVisible.value = true
}

function closeDetail() {
  panelVisible.value = false
  setTimeout(() => { selectedEnt.value = null }, 300)
}

function toggleWishlist(ent) {
  const id = ent.enterpriseId
  const idx = wishList.value.indexOf(id)
  if (idx > -1) { wishList.value.splice(idx, 1); ent.inWishlist = false }
  else { wishList.value.push(id); ent.inWishlist = true }
}

async function handleExport() {
  exporting.value = true
  try {
    const reportId = await reportsService.generateReport({
      type: 'investment_list',
      format: 'Excel',
      weakness: filters.weakness !== 'all' ? filters.weakness : undefined,
      matchMin: filters.minMatch !== 'all' ? filters.minMatch : undefined,
      scale: filters.scale !== 'all' ? filters.scale : undefined,
      keyword: filters.keyword || undefined,
      sortBy: filters.sortBy
    })
    // 跳转到招商报告页，高亮刚生成的报告
    sessionStorage.setItem('navContext', JSON.stringify({ newReportId: reportId }))
    router.push('/reports')
  } catch (e) {
    alert('导出失败，请稍后重试。')
  } finally {
    exporting.value = false
  }
}

async function handleRematch() {
  rematching.value = true
  try {
    await investmentService.rematch()
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
