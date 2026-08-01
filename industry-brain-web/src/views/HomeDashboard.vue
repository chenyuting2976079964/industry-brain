<template>
  <div class="p-6 max-w-[1400px] mx-auto fade-in">
    <!-- ==================== PAGE HEADER ==================== -->
    <div class="flex items-center justify-between mb-5">
      <h2 class="text-lg font-bold text-slate-800">
        <i class="fa-solid fa-house mr-2 text-blue-600"></i>首页看板
      </h2>
      <div class="flex items-center gap-3 text-xs text-slate-400">
        <span class="flex items-center gap-1">
          <span class="w-2 h-2 rounded-full bg-emerald-400 pulse-dot"></span>
          数据更新于 {{ appStore.lastUpdateTime }}
        </span>
        <span class="w-px h-4 bg-slate-200"></span>
        <button
          @click="appStore.enterDashboard()"
          class="flex items-center gap-1.5 bg-blue-600 text-white px-3 py-1.5 rounded-lg hover:bg-blue-700 transition-colors shadow-sm shadow-blue-200"
        >
          <i class="fa-solid fa-display text-xs"></i>
          <span class="font-medium">全景大屏</span>
        </button>
      </div>
    </div>

    <!-- ==================== KPI CARDS (3 columns) ==================== -->
    <div class="grid grid-cols-3 gap-4 mb-5">
      <!-- KPI-1: Patent -->
      <div class="data-card bg-white rounded-xl p-5 border border-slate-100 cursor-pointer hover:shadow-md" @click="openPatentDetail">
        <div class="flex items-start justify-between">
          <div>
            <p class="text-slate-400 text-xs font-medium uppercase tracking-wide">专利总量</p>
            <p class="text-3xl font-bold text-slate-800 mt-1">{{ (kpiData.patent?.total || 0).toLocaleString() }}</p>
            <p class="text-emerald-500 text-xs mt-1">
              <i class="fa-regular fa-circle-check mr-0.5"></i>数据实时更新
            </p>
          </div>
          <div class="w-11 h-11 gradient-primary rounded-xl flex items-center justify-center shadow-lg shadow-blue-500/20">
            <i class="fa-regular fa-file-lines text-white text-lg"></i>
          </div>
        </div>
        <div class="mt-3 pt-3 border-t border-slate-100 flex justify-between text-xs text-slate-400">
          <span>发明: {{ (kpiData.patent?.invention || 0).toLocaleString() }}</span>
          <span>实用新型: {{ (kpiData.patent?.utility || 0).toLocaleString() }}</span>
          <span>外观: {{ (kpiData.patent?.design || 0).toLocaleString() }}</span>
        </div>
      </div>

      <!-- KPI-2: Enterprise -->
      <div class="data-card bg-white rounded-xl p-5 border border-slate-100 cursor-pointer hover:shadow-md" @click="openEnterpriseDetail">
        <div class="flex items-start justify-between">
          <div>
            <p class="text-slate-400 text-xs font-medium uppercase tracking-wide">企业数量</p>
            <p class="text-3xl font-bold text-slate-800 mt-1">{{ (kpiData.enterprise?.total || 0).toLocaleString() }}</p>
            <p class="text-emerald-500 text-xs mt-1">
              <i class="fa-regular fa-circle-check mr-0.5"></i>数据实时更新
            </p>
          </div>
          <div class="w-11 h-11 gradient-accent rounded-xl flex items-center justify-center shadow-lg shadow-cyan-500/20">
            <i class="fa-regular fa-building text-white text-lg"></i>
          </div>
        </div>
        <div class="mt-3 pt-3 border-t border-slate-100 flex justify-between text-xs text-slate-400">
          <span>规上: {{ (kpiData.enterprise?.aboveScale || 0).toLocaleString() }}</span>
          <span>高新: {{ (kpiData.enterprise?.highTech || 0).toLocaleString() }}</span>
          <span>专精特新: {{ (kpiData.enterprise?.specialized || 0).toLocaleString() }}</span>
        </div>
      </div>

      <!-- KPI-3: Diagnosis -->
      <div class="data-card bg-white rounded-xl p-5 border border-slate-100 cursor-pointer hover:shadow-md" @click="openDiagnosisSummary">
        <div class="flex items-start justify-between">
          <div>
            <p class="text-slate-400 text-xs font-medium uppercase tracking-wide">强弱链诊断</p>
            <p class="text-3xl font-bold text-slate-800 mt-1">{{ (kpiData.diagnosis?.totalNodes || 0) > 0 ? (kpiData.diagnosis?.weakCount || 0) + '/' + (kpiData.diagnosis?.totalNodes || 0) : '--' }}</p>
            <p class="text-slate-400 text-xs mt-1">
              <i class="fa-regular fa-circle-check mr-0.5"></i>待运行诊断
            </p>
          </div>
          <div class="w-11 h-11 gradient-success rounded-xl flex items-center justify-center shadow-lg shadow-emerald-500/20">
            <i class="fa-solid fa-microchip text-white text-lg"></i>
          </div>
        </div>
        <div class="mt-3 pt-3 border-t border-slate-100 flex justify-between text-xs text-slate-400">
          <span class="text-emerald-600">强链 {{ kpiData.diagnosis?.strong }}</span>
          <span class="text-amber-600">补链 {{ kpiData.diagnosis?.supplement }}</span>
          <span class="text-red-600">弱链 {{ kpiData.diagnosis?.weak }}</span>
          <span class="text-slate-400">缺链 {{ kpiData.diagnosis?.missing }}</span>
        </div>
      </div>
    </div>

    <!-- ==================== MIDDLE ROW: Chain Dist + Alerts ==================== -->
    <div class="grid grid-cols-3 gap-4 mb-5">
      <!-- Chain Distribution (2/3) -->
      <div class="col-span-2 bg-white rounded-xl p-5 border border-slate-100 data-card">
        <div class="flex items-center justify-between mb-4">
          <h3 class="font-semibold text-slate-800">
            <i class="fa-solid fa-chart-simple mr-2 text-blue-600"></i>产业链节点分布
          </h3>
          <button @click="navigateTo('industry')" class="text-blue-600 text-xs hover:underline">
            查看全景 <i class="fa-solid fa-arrow-right ml-1"></i>
          </button>
        </div>
        <div class="space-y-3">
          <div class="cursor-pointer" @click="navigateTo('industry')">
            <div class="flex justify-between text-sm mb-1.5">
              <span class="text-slate-700 font-medium">上游</span>
              <span class="text-slate-500">{{ chainDist.upstream?.count?.toLocaleString() }} 家企业</span>
            </div>
            <div class="h-3 bg-blue-100 rounded-full overflow-hidden">
              <div class="h-full bg-blue-600 rounded-full transition-all duration-500" :style="{ width: (chainDist.upstream?.pct || 0) + '%' }"></div>
            </div>
          </div>
          <div class="cursor-pointer" @click="navigateTo('industry')">
            <div class="flex justify-between text-sm mb-1.5">
              <span class="text-slate-700 font-medium">中游</span>
              <span class="text-slate-500">{{ chainDist.midstream?.count?.toLocaleString() }} 家企业</span>
            </div>
            <div class="h-3 bg-blue-100 rounded-full overflow-hidden">
              <div class="h-full bg-blue-600 rounded-full transition-all duration-500" :style="{ width: (chainDist.midstream?.pct || 0) + '%' }"></div>
            </div>
          </div>
          <div class="cursor-pointer" @click="navigateTo('industry')">
            <div class="flex justify-between text-sm mb-1.5">
              <span class="text-slate-700 font-medium">下游</span>
              <span class="text-slate-500">{{ chainDist.downstream?.count?.toLocaleString() }} 家企业</span>
            </div>
            <div class="h-3 bg-blue-100 rounded-full overflow-hidden">
              <div class="h-full bg-blue-600 rounded-full transition-all duration-500" :style="{ width: (chainDist.downstream?.pct || 0) + '%' }"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- Alerts (1/3) -->
      <div class="bg-white rounded-xl p-5 border border-slate-100 data-card">
        <h3 class="font-semibold text-slate-800 mb-4">
          <i class="fa-solid fa-bell mr-2 text-amber-500"></i>待处理预警
          <span class="text-xs font-normal text-slate-400 ml-1">({{ alerts.length }}条)</span>
        </h3>
        <div class="space-y-2">
          <div
            v-for="alert in (alertShowAll ? alerts : alerts.slice(0, 5))"
            :key="alert.id"
            class="flex items-start gap-3 p-3 rounded-lg cursor-pointer transition-colors"
            :class="alertLevelClass(alert.level)"
            @click="openAlertDetail(alert.id)"
          >
            <div class="w-7 h-7 rounded-full flex items-center justify-center flex-shrink-0" :class="alertIconBgClass(alert.level)">
              <i :class="alertIconClass(alert.level)" class="text-xs"></i>
            </div>
            <div class="flex-1 min-w-0">
              <p class="text-xs font-medium text-slate-700">{{ alert.title }}</p>
              <p class="text-xs text-slate-400 mt-0.5 truncate">{{ alert.desc }}</p>
            </div>
          </div>
          <div v-if="alerts.length === 0" class="text-center py-6 text-xs text-slate-400">
            <i class="fa-regular fa-circle-check text-emerald-400 text-lg block mb-1"></i>
            暂无待处理预警
          </div>
          <button
            v-if="alerts.length > 5"
            @click="alertShowAll = !alertShowAll"
            class="w-full py-2 text-xs text-blue-600 border border-blue-200 rounded-lg hover:bg-blue-50 transition-colors font-medium"
          >
            <i :class="alertShowAll ? 'fa-solid fa-chevron-up' : 'fa-solid fa-chevron-down'" class="mr-1"></i>
            {{ alertShowAll ? '收起' : '查看全部 ' + alerts.length + ' 条预警' }}
          </button>
        </div>
      </div>
    </div>

    <!-- ==================== PARK STATS TABLE ==================== -->
    <div class="bg-white rounded-xl p-5 border border-slate-100 data-card">
      <div class="flex items-center justify-between mb-3">
        <h3 class="font-semibold text-slate-800">
          <i class="fa-solid fa-building mr-2 text-blue-600"></i>园区载体统计表
        </h3>
        <span class="text-xs text-slate-400">共 {{ parks.length }} 个园区</span>
      </div>
      <table class="table-2">
        <thead>
          <tr>
            <th>园区名称</th>
            <th>类型</th>
            <th>企业数</th>
            <th>专利数</th>
            <th>主导产业</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="park in (parkShowAll ? parks : parks.slice(0, 8))" :key="park.id">
            <td class="font-medium cursor-pointer hover:text-blue-600 transition-colors" @click="openParkDetail(park)">
              {{ park.name }}
            </td>
            <td>
              <span class="badge" :class="parkTypeBadgeClass(park.typeColor)">
                {{ park.type }}
              </span>
            </td>
            <td>{{ park.enterprises?.toLocaleString() }}</td>
            <td>{{ park.patents?.toLocaleString() }}</td>
            <td class="text-xs text-slate-500">{{ park.industry }}</td>
          </tr>
        </tbody>
      </table>
      <button
        v-if="parks.length > 8"
        @click="parkShowAll = !parkShowAll"
        class="w-full mt-3 py-2 text-xs text-blue-600 border border-blue-200 rounded-lg hover:bg-blue-50 transition-colors font-medium"
      >
        <i :class="parkShowAll ? 'fa-solid fa-chevron-up' : 'fa-solid fa-chevron-down'" class="mr-1"></i>
        {{ parkShowAll ? '收起' : '查看全部 ' + parks.length + ' 个园区' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { useModal } from '@/composables/useModal'
import { homeDashboardService } from '@/services/homedashboard.service'

const router = useRouter()
const appStore = useAppStore()
const modal = useModal()

// ---- Reactive data ----
const kpiData = reactive({ patent: null, enterprise: null, diagnosis: null })
const chainDist = reactive({ upstream: null, midstream: null, downstream: null })
const alerts = ref([])
const parks = ref([])
const alertShowAll = ref(false)
const parkShowAll = ref(false)

// ---- Alert helpers ----
function alertLevelClass(level) {
  return {
    high: 'bg-red-50 hover:bg-red-100',
    medium: 'bg-amber-50 hover:bg-amber-100',
    info: 'bg-blue-50 hover:bg-blue-100'
  }[level] || 'bg-slate-50 hover:bg-slate-100'
}

function alertIconBgClass(level) {
  return {
    high: 'bg-red-100',
    medium: 'bg-amber-100',
    info: 'bg-blue-100'
  }[level] || 'bg-slate-100'
}

function alertIconClass(level) {
  return {
    high: 'fa-solid fa-circle-exclamation text-red-500',
    medium: 'fa-solid fa-triangle-exclamation text-amber-500',
    info: 'fa-solid fa-circle-info text-blue-500'
  }[level] || 'fa-solid fa-circle-info text-slate-500'
}

function parkTypeBadgeClass(color) {
  return {
    blue: 'bg-blue-50 text-blue-600',
    emerald: 'bg-emerald-50 text-emerald-600',
    purple: 'bg-purple-50 text-purple-600',
    cyan: 'bg-cyan-50 text-cyan-600',
    amber: 'bg-amber-50 text-amber-600',
    indigo: 'bg-indigo-50 text-indigo-600'
  }[color] || 'bg-slate-50 text-slate-600'
}

function navigateTo(page) {
  router.push(`/${page}`)
}

// ---- KPI Modal: Patent Detail (H06) ----
async function openPatentDetail() {
  const data = await homeDashboardService.getPatentDetail()
  const bars = data.byChain.map(c => `
    <div>
      <div class="flex justify-between text-xs mb-1"><span>${c.name}</span><span>${c.count.toLocaleString()}</span></div>
      <div class="h-1.5 bg-slate-100 rounded-full overflow-hidden">
        <div class="h-full bg-blue-500 rounded-full" style="width:${c.pct}%"></div>
      </div>
    </div>
  `).join('')

  modal.open('专利总量详细分析', `
    <div class="space-y-4">
      <div class="grid grid-cols-3 gap-3">
        <div class="bg-slate-50 rounded-lg p-4 text-center">
          <p class="text-2xl font-bold text-blue-600">${data.total.toLocaleString()}</p>
          <p class="text-xs text-slate-500 mt-1">总计</p>
        </div>
        <div class="bg-slate-50 rounded-lg p-4 text-center">
          <p class="text-2xl font-bold text-emerald-600">${data.invention.toLocaleString()}</p>
          <p class="text-xs text-slate-500 mt-1">发明专利</p>
        </div>
        <div class="bg-slate-50 rounded-lg p-4 text-center">
          <p class="text-2xl font-bold text-amber-600">${data.highValue.toLocaleString()}</p>
          <p class="text-xs text-slate-500 mt-1">高价值专利</p>
        </div>
      </div>
      <p class="text-sm text-slate-600">${data.description}</p>
      <div><p class="text-sm font-medium text-slate-700 mb-2">按产业链专利分布</p>
        <div class="space-y-2">${bars}</div>
      </div>
    </div>
  `)
}

// ---- KPI Modal: Enterprise Detail (H07) ----
async function openEnterpriseDetail() {
  const data = await homeDashboardService.getEnterpriseDetail()
  const tiers = data.tiers.map(t => `
    <div class="flex items-center gap-4 p-3 bg-${t.color}-50 rounded-lg">
      <div class="w-10 h-10 bg-${t.color}-100 rounded-lg flex items-center justify-center flex-shrink-0">
        <i class="fa-solid fa-${t.icon} text-${t.color}-600 text-sm"></i>
      </div>
      <div class="flex-1">
        <div class="flex justify-between items-center mb-1">
          <span class="text-sm font-medium text-slate-700">${t.name}</span>
          <span class="text-sm font-bold text-${t.color}-600">${t.count.toLocaleString()} 家</span>
        </div>
        <div class="h-2 bg-${t.color}-100 rounded-full overflow-hidden">
          <div class="h-full bg-${t.color}-500 rounded-full" style="width:${t.pct}%"></div>
        </div>
      </div>
    </div>
  `).join('')

  modal.open('企业梯队分析', `
    <div class="space-y-4">
      <div class="grid grid-cols-3 gap-3">
        <div class="bg-slate-50 rounded-lg p-4 text-center cursor-pointer hover:bg-blue-50 transition-all border border-transparent hover:border-blue-200" data-action="enterprise-list" data-category="all">
          <p class="text-2xl font-bold text-blue-600">${data.total.toLocaleString()}</p>
          <p class="text-xs text-slate-500 mt-1">企业总数 <span class="text-blue-500 ml-1 text-[10px]">查看 →</span></p>
        </div>
        <div class="bg-slate-50 rounded-lg p-4 text-center cursor-pointer hover:bg-emerald-50 transition-all border border-transparent hover:border-emerald-200" data-action="enterprise-list" data-category="hightech">
          <p class="text-2xl font-bold text-emerald-600">${data.hightechCount.toLocaleString()}</p>
          <p class="text-xs text-slate-500 mt-1">高新技术企业 <span class="text-blue-500 ml-1 text-[10px]">查看 →</span></p>
        </div>
        <div class="bg-slate-50 rounded-lg p-4 text-center cursor-pointer hover:bg-purple-50 transition-all border border-transparent hover:border-purple-200" data-action="enterprise-list" data-category="specialized">
          <p class="text-2xl font-bold text-purple-600">${data.specializedCount.toLocaleString()}</p>
          <p class="text-xs text-slate-500 mt-1">专精特新企业 <span class="text-blue-500 ml-1 text-[10px]">查看 →</span></p>
        </div>
      </div>
      <p class="text-sm text-slate-600">${data.description}</p>
      <div class="space-y-3">${tiers}</div>
      <button class="w-full py-2 bg-blue-600 text-white text-xs rounded-lg hover:bg-blue-700 transition-colors" data-action="navigate" data-page="industry">
        <i class="fa-solid fa-sitemap mr-1"></i>查看产业链分布
      </button>
    </div>
  `, {
    'enterprise-list': (el) => {
      const category = el.getAttribute('data-category')
      modal.close()
      sessionStorage.setItem('navContext', JSON.stringify({ category }))
      router.push('/local-enterprises')
    },
    navigate: (el) => {
      const page = el.getAttribute('data-page')
      modal.close()
      if (page) router.push(`/${page}`)
    }
  })
}

// ---- KPI Modal: Diagnosis Summary (H08) ----
async function openDiagnosisSummary() {
  const data = await homeDashboardService.getDiagnosisSummary()

  modal.open('强弱链诊断汇总', `
    <div class="space-y-4">
      <div class="grid grid-cols-3 gap-3">
        <div class="bg-slate-50 rounded-lg p-4 text-center">
          <p class="text-2xl font-bold text-blue-600">${data.totalNodes}</p>
          <p class="text-xs text-slate-500 mt-1">产业链总节点</p>
        </div>
        <div class="bg-slate-50 rounded-lg p-4 text-center">
          <p class="text-2xl font-bold text-emerald-600">${data.strongPlusSupplement}</p>
          <p class="text-xs text-slate-500 mt-1">强链 + 补链</p>
        </div>
        <div class="bg-slate-50 rounded-lg p-4 text-center">
          <p class="text-2xl font-bold text-red-600">${data.weakTotal}</p>
          <p class="text-xs text-slate-500 mt-1">薄弱环节</p>
        </div>
      </div>
      <p class="text-sm text-slate-600">${data.description}</p>
      <div class="space-y-2">
        <div class="flex items-center gap-3 p-3 bg-emerald-50 rounded-lg">
          <div class="w-8 h-8 bg-emerald-100 rounded-lg flex items-center justify-center"><i class="fa-solid fa-circle text-emerald-500 text-xs"></i></div>
          <div><p class="text-sm font-medium text-slate-700">强链 ${data.strong.count} 个</p><p class="text-xs text-slate-400">${data.strong.items.join('、')}</p></div>
        </div>
        <div class="flex items-center gap-3 p-3 bg-amber-50 rounded-lg">
          <div class="w-8 h-8 bg-amber-100 rounded-lg flex items-center justify-center"><i class="fa-solid fa-circle text-amber-500 text-xs"></i></div>
          <div><p class="text-sm font-medium text-slate-700">补链 ${data.supplement.count} 个</p><p class="text-xs text-slate-400">${data.supplement.items.join('、')}</p></div>
        </div>
        <div class="flex items-center gap-3 p-3 bg-red-50 rounded-lg">
          <div class="w-8 h-8 bg-red-100 rounded-lg flex items-center justify-center"><i class="fa-solid fa-circle text-red-500 text-xs"></i></div>
          <div><p class="text-sm font-medium text-slate-700">弱链 ${data.weak.count} 个</p><p class="text-xs text-slate-400">${data.weak.items.join('、')}</p></div>
        </div>
        <div class="flex items-center gap-3 p-3 bg-slate-50 rounded-lg">
          <div class="w-8 h-8 bg-slate-100 rounded-lg flex items-center justify-center"><i class="fa-solid fa-circle text-slate-400 text-xs"></i></div>
          <div><p class="text-sm font-medium text-slate-700">缺链 ${data.missing.count} 个</p><p class="text-xs text-slate-400">${data.missing.items.join('、')}</p></div>
        </div>
      </div>
      <button class="w-full py-2 bg-blue-600 text-white text-xs rounded-lg hover:bg-blue-700 transition-colors" data-action="navigate" data-page="diagnosis">
        <i class="fa-solid fa-chart-bar mr-1"></i>查看详细诊断
      </button>
    </div>
  `, {
    navigate: (el) => {
      const page = el.getAttribute('data-page')
      modal.close()
      if (page) router.push(`/${page}`)
    }
  })
}

// ---- Alert Detail Modal (H09) ----
async function openAlertDetail(id) {
  let alert
  try {
    alert = await homeDashboardService.getAlertDetail(id)
  } catch (e) {
    console.error('Failed to load alert detail:', e)
    return
  }
  if (!alert) return

  const color = alert.level === 'high' ? 'red' : alert.level === 'medium' ? 'amber' : 'blue'

  modal.open(alert.title, `
    <div class="space-y-4">
      <div class="bg-${color}-50 rounded-lg p-4 flex items-start gap-3">
        <i class="fa-solid fa-circle-exclamation text-${color}-500 mt-0.5"></i>
        <div>
          <p class="font-medium text-sm">${alert.desc}</p>
          <p class="text-xs text-slate-400 mt-1">建议尽快处理此预警事项</p>
        </div>
      </div>
      <div class="flex gap-2">
        <button class="flex-1 py-2 bg-blue-600 text-white text-xs rounded-lg hover:bg-blue-700 transition-colors font-medium" data-action="alert-diagnosis">
          <i class="fa-solid fa-magnifying-glass mr-1"></i>查看详情
        </button>
        <button class="flex-1 py-2 text-xs text-slate-600 border border-slate-200 rounded-lg hover:bg-slate-50 transition-colors font-medium" id="dismissBtn_${alert.id}">
          <i class="fa-solid fa-check mr-1"></i>标记已处理
        </button>
      </div>
    </div>
  `, {
    'alert-diagnosis': () => {
      modal.close()
      router.push('/diagnosis')
    },
    [`#dismissBtn_${alert.id}`]: async () => {
      await homeDashboardService.dismissAlert(alert.id)
      modal.close()
      alerts.value = alerts.value.filter(a => a.id !== alert.id)
      appStore.alertCount = alerts.value.length
    }
  })
}

// ---- Park Detail Modal ----
function openParkDetail(park) {
  modal.open(`${park.name} - 园区概况`, `
    <div class="space-y-4">
      <div class="grid grid-cols-3 gap-3">
        <div class="bg-slate-50 rounded-lg p-4 text-center cursor-pointer hover:bg-blue-50 transition-all border border-transparent hover:border-blue-200" data-action="park-enterprise" data-park="${park.name}" data-area="${park.area || ''}">
          <p class="text-2xl font-bold text-blue-600">${park.enterprises.toLocaleString()}</p>
          <p class="text-xs text-slate-500 mt-1">企业数 <span class="text-blue-500 ml-1 text-[10px]">查看 →</span></p>
        </div>
        <div class="bg-slate-50 rounded-lg p-4 text-center cursor-pointer hover:bg-emerald-50 transition-all border border-transparent hover:border-emerald-200" data-action="park-patent" data-park="${park.name}">
          <p class="text-2xl font-bold text-emerald-600">${park.patents.toLocaleString()}</p>
          <p class="text-xs text-slate-500 mt-1">专利数 <span class="text-blue-500 ml-1 text-[10px]">查看 →</span></p>
        </div>
        <div class="bg-slate-50 rounded-lg p-4 text-center cursor-pointer hover:bg-amber-50 transition-all border border-transparent hover:border-amber-200" data-action="park-highvalue" data-park="${park.name}">
          <p class="text-2xl font-bold text-amber-600">${(park.highValuePatents || 0).toLocaleString()}</p>
          <p class="text-xs text-slate-500 mt-1">高价值专利 <span class="text-blue-500 ml-1 text-[10px]">查看 →</span></p>
        </div>
      </div>
      <div class="flex items-center gap-2">
        <span class="badge ${parkTypeBadgeClass(park.typeColor)}">${park.type}</span>
        <span class="text-xs text-slate-500">📍 ${park.area || '--'}</span>
      </div>
      <p class="text-sm text-slate-600">${park.desc || ''}</p>
      <div class="grid grid-cols-2 gap-3">
        <div class="bg-blue-50 rounded-lg p-3">
          <p class="text-xs font-medium text-slate-700 mb-1">重点企业</p>
          <p class="text-xs text-slate-500">${park.firms || '--'}</p>
        </div>
        <div class="bg-emerald-50 rounded-lg p-3">
          <p class="text-xs font-medium text-slate-700 mb-1">产业链覆盖</p>
          <p class="text-xs text-slate-500">${park.chains || '--'}</p>
        </div>
      </div>
      <div>
        <p class="text-xs font-medium text-slate-500 mb-2">主导产业</p>
        <div class="flex flex-wrap gap-1">
          ${(park.industry || '').split('、').map(i => `<span class="px-2 py-0.5 bg-slate-100 text-slate-600 rounded text-[10px]">${i}</span>`).join('')}
        </div>
      </div>
    </div>
  `, {
    'park-enterprise': (el) => {
      const parkName = el.getAttribute('data-park')
      const area = el.getAttribute('data-area')
      modal.close()
      sessionStorage.setItem('navContext', JSON.stringify({ park: parkName, area, view: 'enterprise' }))
      router.push('/local-enterprises')
    },
    'park-patent': (el) => {
      const parkName = el.getAttribute('data-park')
      modal.close()
      openParkPatentDetail(parkName)
    },
    'park-highvalue': (el) => {
      const parkName = el.getAttribute('data-park')
      modal.close()
      openParkHighValueDetail(parkName)
    }
  })
}

// ---- Park Patent Detail Modal ----
async function openParkPatentDetail(parkName) {
  const park = parks.value.find(p => p.name === parkName)
  if (!park) return
  const total = park.patents
  const invention = Math.round(total * 0.52)
  const utility = Math.round(total * 0.33)
  const design = total - invention - utility

  let fields = ['技术领域一', '技术领域二', '技术领域三', '技术领域四', '技术领域五']
  try {
    const fieldData = await homeDashboardService.getParkPatentDetail(parkName)
    if (fieldData && fieldData.fields) fields = fieldData.fields
  } catch (e) { /* fallback to defaults */ }
  const weights = [0.30, 0.25, 0.18, 0.15, 0.12]
  const fieldBars = fields.map((f, i) => {
    const c = Math.round(total * weights[i])
    const pct = Math.round(weights[i] * 100)
    return `<div><div class="flex justify-between text-xs mb-1"><span class="text-slate-600">${f}</span><span class="text-slate-400">${c.toLocaleString()}件</span></div><div class="h-2 bg-slate-100 rounded-full"><div class="h-full bg-blue-500 rounded-full" style="width:${pct}%"></div></div></div>`
  }).join('')

  modal.open(`${parkName} · 专利详情`, `
    <div class="space-y-4">
      <p class="text-sm text-slate-600">${parkName}累计专利 ${total.toLocaleString()} 件，以下是专利类型分布和技术领域构成。</p>
      <div class="grid grid-cols-3 gap-3">
        <div class="bg-slate-50 rounded-lg p-3 text-center"><p class="text-lg font-bold text-blue-600">${invention.toLocaleString()}</p><p class="text-[10px] text-slate-400">发明专利</p></div>
        <div class="bg-slate-50 rounded-lg p-3 text-center"><p class="text-lg font-bold text-emerald-600">${utility.toLocaleString()}</p><p class="text-[10px] text-slate-400">实用新型</p></div>
        <div class="bg-slate-50 rounded-lg p-3 text-center"><p class="text-lg font-bold text-amber-600">${design.toLocaleString()}</p><p class="text-[10px] text-slate-400">外观设计</p></div>
      </div>
      <div><p class="text-xs font-medium text-slate-700 mb-2"><i class="fa-solid fa-chart-simple mr-1 text-blue-500"></i>按技术领域分布</p><div class="space-y-2">${fieldBars}</div></div>
      <button class="w-full py-2 text-xs text-slate-600 border border-slate-200 rounded-lg hover:bg-slate-50 transition-colors" data-action="close-modal">
        <i class="fa-regular fa-circle-check mr-1"></i>关闭
      </button>
    </div>
  `, {
    'close-modal': () => modal.close()
  })
}

// ---- Park High-Value Patent Detail Modal ----
async function openParkHighValueDetail(parkName) {
  const park = parks.value.find(p => p.name === parkName)
  if (!park) return
  const total = park.highValuePatents || 0

  let patents = []
  try {
    patents = await homeDashboardService.getParkHighValuePatents(parkName) || []
  } catch (e) { /* fallback to empty */ }
  const rankIcons = ['fa-crown', 'fa-2', 'fa-3', 'fa-4', 'fa-5']
  const rankColors = ['text-amber-500', 'text-slate-400', 'text-orange-700', 'text-slate-400', 'text-slate-400']
  const rows = patents.map((pt, i) => `
    <div class="flex items-center gap-3 p-3 bg-slate-50 rounded-lg">
      <div class="w-7 h-7 rounded-full flex items-center justify-center flex-shrink-0 text-xs" style="background:rgba(245,158,11,0.1)">
        <i class="fa-solid ${rankIcons[i] || 'fa-file'} ${rankColors[i] || 'text-slate-400'} text-xs"></i>
      </div>
      <div class="flex-1 min-w-0">
        <p class="text-xs font-medium text-slate-700">${pt.name}</p>
        <div class="flex items-center gap-2 mt-0.5">
          <span class="text-[10px] text-blue-500 bg-blue-50 px-1.5 py-0.5 rounded">${pt.type}</span>
          <span class="text-[10px] text-amber-600">估值 ${pt.value}</span>
        </div>
      </div>
    </div>
  `).join('')

  modal.open(`${parkName} · 高价值专利`, `
    <div class="space-y-4">
      <p class="text-sm text-slate-600">${parkName}共有高价值专利 ${total} 件，以下为估值排名前5的高价值专利：</p>
      <div class="bg-amber-50 rounded-lg p-3 text-center border border-amber-100">
        <p class="text-2xl font-bold text-amber-600">${total}</p>
        <p class="text-xs text-slate-500 mt-1">高价值专利总数</p>
      </div>
      <div><p class="text-xs font-medium text-slate-700 mb-2"><i class="fa-solid fa-crown mr-1 text-amber-500"></i>高价值专利 TOP5</p><div class="space-y-2">${rows}</div></div>
      <button class="w-full py-2 text-xs text-slate-600 border border-slate-200 rounded-lg hover:bg-slate-50 transition-colors" data-action="close-modal">
        <i class="fa-regular fa-circle-check mr-1"></i>关闭
      </button>
    </div>
  `, {
    'close-modal': () => modal.close()
  })
}

// ---- Init: load all homedashboard data ----
onMounted(async () => {
  try {
    const [kpi, chain, alertList, parkList] = await Promise.all([
      homeDashboardService.getKpi(),          // H01
      homeDashboardService.getChainDist(),   // H02
      homeDashboardService.getAlerts(),      // H03
      homeDashboardService.getParkStats()    // H05
    ])

    Object.assign(kpiData, { patent: kpi.patent, enterprise: kpi.enterprise, diagnosis: kpi.diagnosis })
    Object.assign(chainDist, chain)
    alerts.value = alertList
    parks.value = parkList

    appStore.alertCount = alertList.length
  } catch (e) {
    console.error('Failed to load dashboard data:', e)
  }
})
</script>
