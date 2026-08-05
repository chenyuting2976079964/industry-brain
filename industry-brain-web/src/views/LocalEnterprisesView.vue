<template>
  <div class="p-6 max-w-[1400px] mx-auto fade-in">
    <!-- Page Header -->
    <div class="flex items-center justify-between mb-5">
      <h2 class="text-lg font-bold text-slate-800"><i class="fa-solid fa-building mr-2 text-blue-600"></i>本地企业查询</h2>
      <div class="flex items-center gap-2 text-xs text-slate-400">
        <span><i class="fa-regular fa-database mr-1"></i>共 <strong class="text-blue-600">{{ filteredEnterprises.length }}</strong> 家企业</span>
      </div>
    </div>

    <!-- Filter Card -->
    <div class="bg-white rounded-xl border border-slate-100 p-5 mb-4 data-card">
      <div class="flex items-center gap-3 mb-4 flex-wrap">
        <div class="flex items-center gap-2">
          <span class="text-xs font-medium text-slate-600 whitespace-nowrap">区县</span>
          <select v-model="filters.district" class="select-field text-xs w-36" @change="applyFilters">
            <option value="">全部区县</option>
            <option v-for="d in districtOptions" :key="d" :value="d">{{ d }}</option>
          </select>
        </div>
        <div class="flex items-center gap-2">
          <span class="text-xs font-medium text-slate-600 whitespace-nowrap">企业类型</span>
          <select v-model="filters.type" class="select-field text-xs w-36" @change="applyFilters">
            <option value="">全部类型</option>
            <option v-for="t in typeOptions" :key="t" :value="t">{{ t }}</option>
          </select>
        </div>
        <div class="flex-1 min-w-[200px]">
          <div class="relative">
            <input v-model="filters.search" type="text" placeholder="搜索企业名称..." class="input-field text-xs" style="padding-left:30px" @input="applyFilters" />
            <i class="fa-solid fa-search absolute left-2.5 top-1/2 -translate-y-1/2 text-slate-300 text-xs"></i>
          </div>
        </div>
        <button class="btn-outline text-xs" @click="resetFilters"><i class="fa-solid fa-rotate-left mr-1"></i>重置</button>
      </div>
      <!-- Active filter tags -->
      <div v-if="activeTags.length > 0" class="flex items-center gap-2 flex-wrap">
        <span
          v-for="tag in activeTags"
          :key="tag.key"
          class="inline-flex items-center gap-1 px-2.5 py-1 bg-blue-50 text-blue-600 rounded-full text-[11px] cursor-pointer"
          @click="removeFilter(tag.key)"
        >
          {{ tag.label }}<i class="fa-solid fa-xmark hover:text-blue-800"></i>
        </span>
      </div>
    </div>

    <!-- Results bar -->
    <div class="flex items-center justify-between mb-3">
      <p class="text-sm text-slate-500">共 <strong class="text-blue-600">{{ filteredEnterprises.length }}</strong> 条结果</p>
      <div class="flex items-center gap-2 text-xs text-slate-400">
        <span :class="['cursor-pointer', viewMode === 'grid' ? 'text-blue-600' : 'hover:text-blue-600']" @click="viewMode = 'grid'"><i class="fa-solid fa-table-cells"></i></span>
        <span :class="['cursor-pointer', viewMode === 'list' ? 'text-blue-600' : 'hover:text-blue-600']" @click="viewMode = 'list'"><i class="fa-solid fa-list"></i></span>
      </div>
    </div>

    <!-- Empty state -->
    <div v-if="filteredEnterprises.length === 0" class="text-center py-16 text-slate-400">
      <i class="fa-solid fa-building-circle-exclamation text-4xl mb-3"></i>
      <p class="text-sm">未找到匹配的企业，请调整筛选条件</p>
    </div>

    <!-- Grid View -->
    <div v-else-if="viewMode === 'grid'" class="grid grid-cols-2 gap-3">
      <div
        v-for="(e, i) in pagedEnterprises"
        :key="e.name"
        class="ent-card bg-white rounded-xl border border-slate-100 p-4 hover:shadow-md cursor-pointer"
        @click="viewEnterprise(e.name)"
      >
        <div class="flex items-start gap-3 mb-2">
          <div :class="['w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0', 'bg-' + colors[i % colors.length] + '-100']">
            <span :class="['text-' + colors[i % colors.length] + '-600 font-bold text-sm']">{{ e.name.charAt(0) }}</span>
          </div>
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-1.5 min-w-0">
              <span class="text-sm font-semibold text-slate-800 truncate">{{ e.name }}</span>
              <span :class="['badge text-[10px]', typeClass(e.type)]">{{ e.type }}</span>
            </div>
            <p class="text-xs text-slate-400 mt-0.5">
              <i class="fa-solid fa-location-dot mr-0.5"></i>{{ e.loc }}
            </p>
          </div>
          <i class="fa-solid fa-chevron-right text-slate-300 text-xs mt-2"></i>
        </div>
        <p class="text-xs text-slate-500 line-clamp-2 mb-2">{{ e.desc }}</p>
        <div class="grid grid-cols-3 gap-2 text-center border-t border-slate-100 pt-2">
          <div><p class="text-xs font-bold text-blue-600">{{ e.p }}</p><p class="text-[10px] text-slate-400">专利总量</p></div>
          <div><p class="text-xs font-bold text-emerald-600">{{ e.i }}</p><p class="text-[10px] text-slate-400">发明专利</p></div>
          <div><p class="text-xs font-bold text-amber-600">{{ e.h }}</p><p class="text-[10px] text-slate-400">高价值</p></div>
        </div>
      </div>
    </div>

    <!-- List View -->
    <div v-else class="grid grid-cols-1 gap-2">
      <div
        v-for="(e, i) in pagedEnterprises"
        :key="e.name"
        class="ent-card bg-white rounded-xl border border-slate-100 p-3 hover:shadow-md flex items-center gap-4 cursor-pointer"
        @click="viewEnterprise(e.name)"
      >
        <div :class="['w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0', 'bg-' + colors[i % colors.length] + '-100']">
          <span :class="['text-' + colors[i % colors.length] + '-600 font-bold text-sm']">{{ e.name.charAt(0) }}</span>
        </div>
        <div class="flex-1 min-w-0">
          <div class="flex items-center gap-2">
            <span class="text-sm font-semibold text-slate-800">{{ e.name }}</span>
            <span :class="['badge text-[10px]', typeClass(e.type)]">{{ e.type }}</span>
            <span class="badge bg-slate-100 text-slate-500 text-[10px]">{{ e.tag }}</span>
          </div>
          <p class="text-xs text-slate-400">
            <i class="fa-solid fa-location-dot mr-0.5"></i>{{ e.loc }}
            <span class="mx-1">|</span>专利 {{ e.p }}
            <span class="mx-1">|</span>发明 {{ e.i }}
          </p>
        </div>
        <i class="fa-solid fa-chevron-right text-slate-300 text-xs"></i>
      </div>
    </div>

    <!-- Pagination -->
    <div v-if="filteredEnterprises.length > pageSize" class="flex items-center justify-center gap-1 mt-6">
      <button class="page-btn" :disabled="currentPage === 1" @click="changePage(currentPage - 1)">
        <i class="fa-solid fa-chevron-left"></i>
      </button>
      <button
        v-for="p in pageNumbers"
        :key="p"
        :class="['page-btn', { active: p === currentPage }]"
        @click="changePage(p)"
      >{{ p }}</button>
      <button class="page-btn" :disabled="currentPage === totalPages" @click="changePage(currentPage + 1)">
        <i class="fa-solid fa-chevron-right"></i>
      </button>
      <span class="ml-3 text-xs text-slate-400">共 {{ filteredEnterprises.length }} 家</span>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth'
import { getLocalEnterprises } from '@/services/enterprise.service'
import { get } from '@/services/api'

const router = useRouter()
const appStore = useAppStore()
const authStore = useAuthStore()

appStore.setPage('查看本地企业', 'local-enterprises')

// ─── Enterprise data (loaded from API) ──────────────────────
const enterprises = ref([])

const colors = ['blue', 'purple', 'emerald', 'amber', 'cyan', 'indigo', 'rose', 'teal', 'orange', 'pink', 'violet', 'sky']

// ─── State ─────────────────────────────────────────────────
const viewMode = ref('grid')
const filters = reactive({
  district: '',
  type: '',
  search: ''
})

// 区县下拉选项（根据当前登录城市从区域API获取）
const districtOptions = ref([])

// 企业类型选项
const typeOptions = ['龙头企业', '高新技术企业', '科技型中小企业', '其他']

// ─── Type class helper ─────────────────────────────────────
function typeClass(type) {
  switch (type) {
    case '龙头企业': return 'tag-leading'
    case '专精特新': return 'tag-specialized'
    case '高新技术企业': return 'tag-hightech'
    case '科技型中小企业': return 'tag-sme'
    default: return 'tag-other'
  }
}

// ─── Map API field names to frontend format ────────────────
function mapEnterprise(e) {
  const locVal = e.district || e.registered_address || ''
  return {
    name: e.name,
    loc: locVal,
    type: e.type || e.enterprise_type || '',
    tag: e.tag || '',
    desc: e.description || e.business_status || '',
    p: e.patentCount || e.patent_count || 0,
    i: e.highValueCount || e.high_value_patent_count || 0,
    h: e.highValueCount || e.high_value_patent_count || 0,
    park: e.park || ''
  }
}

// ─── Computed ──────────────────────────────────────────────
const filteredEnterprises = computed(() => {
  return (enterprises.value || []).filter(e => {
    if (filters.district && e.loc !== filters.district) return false
    if (filters.type && e.type !== filters.type) return false
    if (filters.search && !e.name.toLowerCase().includes(filters.search.toLowerCase())) return false
    return true
  })
})

// ─── Pagination ────────────────────────────────────────────
const pageSize = 50
const currentPage = ref(1)

const totalPages = computed(() => Math.max(1, Math.ceil(filteredEnterprises.value.length / pageSize)))

const pagedEnterprises = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredEnterprises.value.slice(start, start + pageSize)
})

const pageNumbers = computed(() => {
  const total = totalPages.value
  const cur = currentPage.value
  const pages = []
  // 显示窗口: 当前页前后各2页 + 首尾
  const want = new Set([1, total, cur - 2, cur - 1, cur, cur + 1, cur + 2])
  let prev = 0
  for (let i = 1; i <= total; i++) {
    if (want.has(i)) {
      if (i - prev > 1) pages.push('...')
      pages.push(i)
      prev = i
    }
  }
  return pages
})

function changePage(p) {
  if (p < 1 || p > totalPages.value) return
  currentPage.value = p
}

const activeTags = computed(() => {
  const tags = []
  if (filters.district) tags.push({ key: 'district', label: '地区: ' + filters.district })
  if (filters.type) tags.push({ key: 'type', label: '类型: ' + filters.type })
  if (filters.search) tags.push({ key: 'search', label: '搜索: "' + filters.search + '"' })
  return tags
})

// ─── Methods ───────────────────────────────────────────────
function applyFilters() {
  currentPage.value = 1  // 筛选变化回到第一页
}

function resetFilters() {
  filters.district = ''
  filters.type = ''
  filters.search = ''
  currentPage.value = 1
}

function removeFilter(key) {
  if (key === 'district') filters.district = ''
  else if (key === 'type') filters.type = ''
  else if (key === 'search') filters.search = ''
  currentPage.value = 1
}

function viewEnterprise(name) {
  sessionStorage.setItem('navContext', JSON.stringify({ enterpriseName: name }))
  router.push('/enterprise')
}

// ─── Init: load enterprises from API and read filters from navContext ─────
onMounted(async () => {
  // 先加载区县列表（根据登录用户所在城市）
  try {
    const regionCode = {
      '马鞍山市': 340500, '合肥市': 340100, '芜湖市': 340200, '南京市': 320100,
      '北京市': 110000, '上海市': 310000, '广州市': 440100, '深圳市': 440300
    }[authStore.region] || 340500
    const res = await get(`/admin/regions?parentId=${regionCode}`)
    if (Array.isArray(res)) {
      districtOptions.value = res.map(r => r.name).filter(n => !n.endsWith('市'))
    }
  } catch (e) { console.error('加载区县失败:', e) }

  // Load enterprises from API
  try {
    const res = await getLocalEnterprises()
    enterprises.value = (res && res.list ? res.list : (Array.isArray(res) ? res : [])).map(mapEnterprise)
  } catch (e) {
    console.error('Failed to load local enterprises:', e)
  }

  try {
    const ctx = JSON.parse(sessionStorage.getItem('navContext')) || {}
    if (ctx.filter) {
      const categoryMap = { sme: '科技型中小企业', other: '其他', hightech: '高新技术企业', specialized: '专精特新', leading: '龙头企业' }
      if (ctx.filter.category && categoryMap[ctx.filter.category]) {
        filters.type = categoryMap[ctx.filter.category]
      }
      if (ctx.filter.district) filters.district = ctx.filter.district
      sessionStorage.removeItem('navContext')
    }
  } catch (e) { /* ignore */ }
})
</script>

<style scoped>
.ent-card { transition: all .2s; cursor: pointer; }
.ent-card:hover { transform: translateY(-2px); box-shadow: 0 8px 25px rgba(0, 0, 0, .1); }

.page-btn {
  min-width: 34px;
  height: 34px;
  padding: 0 8px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  color: #64748b;
  font-size: 13px;
  cursor: pointer;
  transition: all .2s;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.page-btn:hover:not(:disabled) { border-color: #93c5fd; color: #1e40af; background: #eff6ff; }
.page-btn.active { background: #1e40af; color: #fff; border-color: #1e40af; }
.page-btn:disabled { opacity: .4; cursor: not-allowed; }
.page-btn[data-ellipsis] { border: none; cursor: default; }

.tag-specialized { background: #f3e8ff; color: #7c3aed; }
.tag-hightech { background: #dbeafe; color: #1d4ed8; }
.tag-leading { background: #fef3c7; color: #d97706; }
.tag-sme { background: #ccfbf1; color: #0d9488; }
.tag-other { background: #f1f5f9; color: #64748b; }

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

.data-card { transition: all .2s; }
.data-card:hover { box-shadow: 0 4px 15px rgba(0, 0, 0, .08); }

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

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
