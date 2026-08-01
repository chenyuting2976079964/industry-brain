<template>
  <div class="p-6 max-w-[1400px] mx-auto fade-in">
    <!-- Breadcrumb -->
    <div class="text-xs text-slate-400 mb-4">
      <a href="javascript:void(0)" class="hover:text-blue-600" @click="router.push('/home')">首页</a><span class="mx-1">/</span>
      <a href="javascript:void(0)" class="hover:text-blue-600" @click="router.push('/diagnosis')">强弱诊断</a><span class="mx-1">/</span>
      <a href="javascript:void(0)" class="hover:text-blue-600" @click="router.push('/investment')">精准招商</a><span class="mx-1">/</span>
      <span class="text-slate-600 font-medium">{{ ent.name }}</span>
    </div>

    <!-- Enterprise Header Card -->
    <div class="bg-white rounded-xl p-6 border border-slate-100 data-card mb-4">
      <div class="flex items-start justify-between">
        <div class="flex items-start gap-4">
          <div :class="['w-16 h-16 rounded-2xl flex items-center justify-center text-white text-2xl font-bold shadow-lg', 'bg-gradient-to-br ' + ent.color]">
            {{ ent.initial }}
          </div>
          <div>
            <div class="flex items-center gap-2 flex-wrap">
              <h2 class="text-xl font-bold text-slate-800">{{ ent.name }}</h2>
              <span v-for="(tag, i) in ent.tags" :key="i" :class="['badge', ent.tagStyles[i % ent.tagStyles.length]]">{{ tag }}</span>
            </div>
            <p class="text-sm text-slate-400 mt-1">{{ ent.desc }}</p>
            <div class="flex items-center gap-5 mt-2 text-xs text-slate-500 flex-wrap">
              <span><i class="fa-regular fa-building mr-1"></i>成立 {{ ent.year }}年</span>
              <span><i class="fa-solid fa-location-dot mr-1"></i>{{ ent.loc }}</span>
              <span><i class="fa-regular fa-user mr-1"></i>{{ ent.size }}</span>
              <span><i class="fa-regular fa-building mr-1"></i>注册资本 {{ ent.capital }}</span>
            </div>
          </div>
        </div>
        <div class="flex gap-2 flex-shrink-0">
          <button :class="['btn-outline text-xs', { 'btn-favorited': isFavorited }]" @click="toggleFavorite">
            <i :class="isFavorited ? 'fa-solid fa-bookmark' : 'fa-regular fa-bookmark'"></i> {{ isFavorited ? '已收藏' : '收藏' }}
          </button>
          <button class="btn-primary text-xs" @click="showReportModal"><i class="fa-regular fa-file-lines"></i> 下载报告</button>
        </div>
      </div>

      <!-- 企业简介 -->
      <div v-if="ent.intro" class="mt-4 pt-4 border-t border-slate-100">
        <h4 class="text-xs font-semibold text-slate-500 mb-2"><i class="fa-solid fa-building mr-1 text-blue-500"></i>企业简介</h4>
        <p class="text-sm text-slate-600 leading-relaxed">{{ ent.intro }}</p>
      </div>
    </div>

    <!-- Tab Navigation -->
    <div class="flex items-center gap-2 mb-4 bg-white rounded-xl p-1.5 border border-slate-100 w-fit">
      <button :class="['tab-btn', { active: activeTab === 'profile' }]" @click="activeTab = 'profile'">
        <i class="fa-solid fa-microchip mr-1.5"></i>技术画像
      </button>
      <button :class="['tab-btn', { active: activeTab === 'patents' }]" @click="activeTab = 'patents'">
        <i class="fa-regular fa-file-lines mr-1.5"></i>专利列表
        <span class="ml-1 text-[10px] opacity-60">{{ ent.totalPatents }}</span>
      </button>
    </div>

    <!-- Tab: 技术画像 -->
    <div v-show="activeTab === 'profile'">
      <div class="bg-white rounded-xl p-5 border border-slate-100 data-card">
        <h3 class="font-semibold text-slate-800 mb-4"><i class="fa-solid fa-microchip mr-2 text-blue-600"></i>技术画像报告</h3>
        <div class="grid grid-cols-3 gap-3 mb-5">
          <div class="bg-slate-50 rounded-lg p-3 text-center cursor-pointer hover:bg-blue-50" @click="showStatModal('专利总数', '含发明专利 ' + ent.invention + ' 项，实用新型、外观设计等 ' + (ent.totalPatents - ent.invention) + ' 项')">
            <p class="text-2xl font-bold text-slate-800">{{ ent.totalPatents }}</p>
            <p class="text-xs text-slate-400">专利总数</p>
          </div>
          <div class="bg-slate-50 rounded-lg p-3 text-center cursor-pointer hover:bg-blue-50" @click="showStatModal('高价值专利', '符合国家知识产权局高价值发明专利认定标准')">
            <p class="text-2xl font-bold text-emerald-600">{{ ent.highValue }}</p>
            <p class="text-xs text-slate-400">高价值专利</p>
          </div>
          <div class="bg-slate-50 rounded-lg p-3 text-center cursor-pointer hover:bg-blue-50" @click="showStatModal('发明专利', '近三年持续申请中')">
            <p class="text-2xl font-bold text-blue-600">{{ ent.invention }}</p>
            <p class="text-xs text-slate-400">发明专利</p>
          </div>
        </div>
        <h4 class="text-sm font-semibold text-slate-700 mb-3">技术优势领域</h4>
        <div class="space-y-2 mb-5">
          <div v-for="(adv, i) in ent.advantages" :key="i" class="cursor-pointer" @click="showStatModal(adv.name, adv.desc)">
            <div class="flex justify-between text-xs mb-1"><span>{{ adv.name }}</span><span class="text-slate-400">{{ adv.count }}</span></div>
            <div class="h-2 bg-blue-100 rounded-full"><div class="h-full bg-blue-600 rounded-full" :style="{ width: barWidths[i] }"></div></div>
          </div>
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div>
            <h4 class="text-sm font-semibold text-slate-700 mb-3">研发方向</h4>
            <div class="flex flex-wrap gap-2">
              <span v-for="(tag, i) in ent.tags" :key="i" class="badge bg-blue-50 text-blue-600 text-xs cursor-pointer hover:bg-blue-100" @click="showStatModal(tag, '重点研发方向')">{{ tag }}</span>
            </div>
          </div>
          <div>
            <h4 class="text-sm font-semibold text-slate-700 mb-3">技术短板</h4>
            <div class="bg-red-50 rounded-lg p-3 border border-red-100 cursor-pointer hover:bg-red-100" @click="showStatModal('技术短板', ent.weaknessDetail)">
              <div class="flex items-start gap-2">
                <i class="fa-solid fa-triangle-exclamation text-red-400 mt-0.5 text-xs"></i>
                <div><p class="text-xs font-medium text-slate-700">{{ ent.weakness }}</p><p class="text-xs text-slate-400 mt-0.5">点击查看详情</p></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Tab: 专利列表 -->
    <div v-show="activeTab === 'patents'">
      <div class="bg-white rounded-xl border border-slate-100 data-card overflow-hidden">
        <div class="p-4 border-b border-slate-100 flex items-center justify-between">
          <h3 class="text-sm font-semibold text-slate-700"><i class="fa-regular fa-file-lines mr-2 text-blue-500"></i>专利清单</h3>
          <div class="flex items-center gap-2">
            <div class="relative">
              <i class="fa-solid fa-search absolute left-2.5 top-1/2 -translate-y-1/2 text-slate-300 text-xs"></i>
              <input v-model="patentSearch" class="input-field text-xs w-48 pl-7" placeholder="搜索专利名称..." @input="currentPatentPage = 1" />
            </div>
            <select v-model="patentTypeFilter" class="select-field text-xs" @change="currentPatentPage = 1">
              <option value="all">全部类型</option>
              <option value="发明">发明</option>
              <option value="实用新型">实用新型</option>
            </select>
          </div>
        </div>
        <table class="table-2">
          <thead><tr><th>专利名称</th><th>类型</th><th>申请日期</th><th>状态</th><th>详情</th></tr></thead>
          <tbody>
            <tr v-if="paginatedPatents.length === 0"><td colspan="5" class="text-center text-slate-400 py-6">无匹配专利</td></tr>
            <tr v-for="(p, i) in paginatedPatents" :key="i" class="patent-row cursor-pointer" @click="showPatentModal(p)">
              <td class="font-medium">{{ p.n }}</td>
              <td><span :class="['badge', p.t === '发明' ? 'bg-blue-50 text-blue-600' : 'bg-cyan-50 text-cyan-600']">{{ p.t }}</span></td>
              <td>{{ p.d }}</td>
              <td><span :class="['badge', p.s === '授权' ? 'status-strong-bg' : 'bg-amber-50 text-amber-600']">{{ p.s }}</span></td>
              <td><span class="text-blue-600 text-xs cursor-pointer hover:underline">详情</span></td>
            </tr>
          </tbody>
        </table>
        <div class="p-4 border-t border-slate-100 flex items-center justify-between">
          <span class="text-xs text-slate-400">{{ patentPageInfo }}</span>
          <div class="flex gap-1">
            <button v-for="p in totalPatentPages" :key="p" :class="['w-7 h-7 rounded text-xs', p === currentPatentPage ? 'bg-blue-600 text-white' : 'bg-white border text-slate-500']" @click="currentPatentPage = p">{{ p }}</button>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { useEventBus } from '@/stores/eventBus'
import { getEnterprise, getEnterpriseProfile, getEnterprisePatents, searchEnterprise, getLocalEnterprises } from '@/services/enterprise.service'

const router = useRouter()
const appStore = useAppStore()
const eventBus = useEventBus()

appStore.setPage('企业详情', 'enterprise')

// ─── Helpers ──────────────────────────────────────────────
function scoreColor(score) {
  if (score >= 80) return 'emerald'
  if (score >= 60) return 'blue'
  return 'cyan'
}

const tagStylePool = [
  'status-strong-bg', 'bg-purple-50 text-purple-600',
  'bg-blue-50 text-blue-600', 'bg-amber-50 text-amber-600',
  'bg-cyan-50 text-cyan-600', 'bg-indigo-50 text-indigo-600'
]

// ─── Data transform ───────────────────────────────────────
function transformEnterprise(apiEnt, profile, patentData) {
  // 标签从 enterprise_tag 表获取（apiEnt.tags）
  const tags = (apiEnt.tags || []).slice(0, 6)

  const advantages = (profile?.advantages || []).map(a => ({
    name: a.name || '未知领域',
    count: `${a.patentCount || 0}项`,
    desc: a.description || (a.name || '未知领域') + '领域'
  }))

  return {
    id: apiEnt.id,
    initial: (apiEnt.avatarLetter || apiEnt.entName?.charAt(0) || '企').toUpperCase(),
    color: 'from-blue-500 to-blue-700',
    name: apiEnt.entName || apiEnt.shortName || '未知企业',
    tags: tags.length > 0 ? tags : ['暂无标签'],
    tagStyles: tags.map((_, i) => tagStylePool[i % tagStylePool.length]),
    desc: [apiEnt.scale, apiEnt.province, apiEnt.city].filter(Boolean).join(' · ') || apiEnt.legalPerson || '',
    intro: apiEnt.description || '',
    loc: [apiEnt.province, apiEnt.city, apiEnt.district].filter(Boolean).join(' ') || '-',
    year: String(apiEnt.establishYear || '-'),
    size: apiEnt.scale || '-',
    capital: apiEnt.registeredCapital || '-',
    totalPatents: profile?.patentStats?.totalPatents || 0,
    highValue: profile?.patentStats?.highValuePatents || 0,
    invention: profile?.patentStats?.inventionPatents || 0,
    techMatch: '—',
    advantages: advantages.length > 0 ? advantages : [{ name: '暂无数据', count: '0项', desc: '暂无详细技术优势数据' }],
    weakness: (profile?.weaknesses || []).length > 0 ? profile.weaknesses[0].fieldName : '暂无',
    weaknessDetail: (profile?.weaknesses || []).length > 0
      ? profile.weaknesses[0].fieldName + '领域存在技术储备不足，建议加大研发投入'
      : '暂无详细数据',
    patents: (patentData?.rows || []).map(p => ({
      n: p.patentName || p.patentNumber || '',
      t: p.patentType || '发明',
      d: p.applicationDate || '',
      s: p.legalStatus || '授权',
      desc: p.ipcMain || ''
    })),
    _raw: { apiEnt, profile, patentData }
  }
}

// ─── State ─────────────────────────────────────────────────
const currentEnt = ref(null)
const activeTab = ref('profile')
const patentSearch = ref('')
const patentTypeFilter = ref('all')
const currentPatentPage = ref(1)
const loading = ref(false)
const isFavorited = ref(false)
const pageSize = 6

const ent = computed(() => currentEnt.value || {
  name: '加载中...',
  initial: '?',
  color: 'from-slate-500 to-slate-700',
  tags: [], tagStyles: [], desc: '', intro: '', loc: '', year: '', size: '', capital: '',
  totalPatents: 0, highValue: 0, invention: 0, techMatch: '',
  advantages: [], weakness: '', weaknessDetail: '',
  patents: []
})

// ─── Computed ──────────────────────────────────────────────
const barWidths = computed(() => {
  const advs = ent.value.advantages || []
  if (advs.length === 0) return []
  const maxCount = Math.max(...advs.map(a => {
    const m = String(a.count || '').match(/(\d+)/)
    return m ? parseInt(m[1]) : 0
  }), 1)
  return advs.map(a => {
    const m = String(a.count || '').match(/(\d+)/)
    const val = m ? parseInt(m[1]) : 0
    return Math.max(10, Math.round(val / maxCount * 92)) + '%'
  })
})

const filteredPatents = computed(() => {
  const search = patentSearch.value.toLowerCase()
  const type = patentTypeFilter.value
  return (ent.value.patents || []).filter(p => {
    const nameMatch = !search || (p.n || '').toLowerCase().includes(search)
    const typeMatch = type === 'all' || p.t === type
    return nameMatch && typeMatch
  })
})

const totalPatentPages = computed(() => Math.max(1, Math.ceil(filteredPatents.value.length / pageSize)))

const paginatedPatents = computed(() => {
  const start = (currentPatentPage.value - 1) * pageSize
  return filteredPatents.value.slice(start, start + pageSize)
})

const patentPageInfo = computed(() => {
  const total = filteredPatents.value.length
  if (total === 0) return '0 / 共 ' + ent.value.totalPatents + ' 项'
  const start = (currentPatentPage.value - 1) * pageSize + 1
  const end = Math.min(start + pageSize - 1, total)
  return start + '-' + end + ' / 共 ' + total + ' 条匹配'
})

// ─── Data loading ─────────────────────────────────────────
async function loadEnterpriseData(entId) {
  loading.value = true
  try {
    const [apiEnt, profile, patentData] = await Promise.all([
      getEnterprise(entId),
      getEnterpriseProfile(entId).catch(() => null),
      getEnterprisePatents(entId, { page: 1, size: 100 }).catch(() => ({ rows: [] }))
    ])
    currentEnt.value = transformEnterprise(apiEnt, profile, patentData)
    currentPatentPage.value = 1
  } catch (e) {
    console.error('Failed to load enterprise data', e)
    currentEnt.value = {
      ...ent.value,
      name: '加载失败',
      desc: e.message || '请检查网络后重试'
    }
  } finally {
    loading.value = false
  }
}

// ─── Modal helpers ─────────────────────────────────────────
function showStatModal(title, desc) {
  eventBus.openModal(title, '<div class="py-2"><p class="text-sm text-slate-600">' + desc + '</p></div>')
}

function showPatentModal(p) {
  const typeColor = p.t === '发明' ? 'text-blue-600' : 'text-cyan-600'
  const statusColor = p.s === '授权' ? 'text-emerald-600' : 'text-amber-600'
  eventBus.openModal(p.n, `
    <div class="space-y-3 py-2">
      <div class="flex items-center gap-4 text-xs text-slate-500">
        <span>类型：<span class="${typeColor} font-medium">${p.t}</span></span>
        <span>申请日：<span class="font-medium">${p.d}</span></span>
        <span>状态：<span class="${statusColor} font-medium">${p.s}</span></span>
      </div>
      <p class="text-sm text-slate-600">${p.desc}</p>
    </div>`)
}

function showReportModal() {
  eventBus.openModal('下载报告', `
    <div class="text-center py-4">
      <i class="fa-regular fa-file-pdf text-slate-300 text-5xl mb-3"></i>
      <p class="text-sm font-medium text-slate-700">${ent.value.name} 完整画像报告</p>
      <p class="text-xs text-slate-400 mt-2">专利布局 · 技术优势 · 匹配度分析 · 招商建议</p>
      <div class="flex gap-2 justify-center mt-4">
        <button class="btn-primary text-xs"><i class="fa-solid fa-download"></i> 下载 PDF</button>
        <button class="btn-outline text-xs"><i class="fa-solid fa-download"></i> 下载 Excel</button>
      </div>
    </div>`)
}

function toggleFavorite() {
  isFavorited.value = !isFavorited.value
  showToast(isFavorited.value ? '已收藏' : '已取消收藏')
}

function showToast(msg) {
  const el = document.createElement('div')
  el.className = 'fixed top-4 right-4 bg-slate-800 text-white text-xs px-4 py-2 rounded-lg shadow-lg z-[999]'
  el.textContent = msg
  document.body.appendChild(el)
  setTimeout(() => el.remove(), 2000)
}

// ─── Mount ─────────────────────────────────────────────────
onMounted(async () => {
  try {
    const ctx = JSON.parse(sessionStorage.getItem('navContext')) || {}
    const entName = ctx.enterpriseName
    sessionStorage.removeItem('navContext')

    if (entName) {
      const results = await searchEnterprise(entName)
      if (results && results.length > 0) {
        await loadEnterpriseData(results[0].id)
        return
      }
    }

    // Fallback: load first local enterprise
    const locals = await getLocalEnterprises()
    if (locals && locals.length > 0) {
      await loadEnterpriseData(locals[0].id)
    }
  } catch (e) {
    console.error('Failed to initialize enterprise page', e)
  }
})
</script>

<style scoped>
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
.btn-favorited { border-color: #f59e0b; color: #d97706; background: #fffbeb; }

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

.patent-row { cursor: pointer; }
.patent-row:hover { background: #f0f5ff; }
</style>
