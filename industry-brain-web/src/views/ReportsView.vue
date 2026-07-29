<template>
  <div class="p-6 max-w-[1400px] mx-auto fade-in">
    <!-- ==================== HEADER ==================== -->
    <div class="flex items-center justify-between mb-5">
      <div>
        <h2 class="text-lg font-bold text-slate-800">
          <i class="fa-regular fa-file-lines mr-2 text-blue-600"></i>招商报告与清单导出
        </h2>
        <p class="text-xs text-slate-400 mt-0.5">一键生成招商推荐清单 · 支持多种格式导出</p>
      </div>
      <div class="flex gap-2">
        <button class="btn-outline text-xs" @click="handleExportAll('excel')">
          <i class="fa-regular fa-file-excel"></i> 导出全部 Excel
        </button>
        <button class="btn-outline text-xs" @click="handleExportAll('pdf')">
          <i class="fa-regular fa-file-pdf"></i> 导出全部 PDF
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

    <!-- ==================== TAB 1: Saved Reports ==================== -->
    <div v-show="activeTab === 'saved'">
      <div class="space-y-2">
        <div
          v-for="report in reports"
          :key="report.id"
          class="bg-white rounded-xl p-4 border border-slate-100 data-card flex items-center justify-between cursor-pointer hover:shadow-md"
          @click="previewReport(report)"
        >
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-xl flex items-center justify-center" :class="report.iconBg">
              <i :class="[report.icon, report.iconColor].join(' ')"></i>
            </div>
            <div>
              <p class="text-sm font-medium text-slate-700">{{ report.name }}</p>
              <p class="text-xs text-slate-400">{{ report.date }} · {{ report.desc }}</p>
            </div>
          </div>
          <div class="flex gap-2">
            <button class="btn-outline text-xs" @click.stop="previewReport(report)">
              <i class="fa-regular fa-eye"></i>
            </button>
            <button class="btn-outline text-xs" @click.stop="downloadReport(report)">
              <i class="fa-solid fa-download"></i>
            </button>
            <button class="btn-outline text-xs text-red-400" @click.stop="handleDeleteReport(report)">
              <i class="fa-regular fa-trash-can"></i>
            </button>
          </div>
        </div>
      </div>

      <!-- Stats Cards -->
      <div class="grid grid-cols-3 gap-4 mt-5">
        <div class="bg-white rounded-xl p-4 border border-slate-100 data-card">
          <p class="text-xs text-slate-400">本月生成报告</p>
          <p class="text-2xl font-bold text-slate-800 mt-1">{{ stats.monthCount }} 份</p>
        </div>
        <div class="bg-white rounded-xl p-4 border border-slate-100 data-card">
          <p class="text-xs text-slate-400">累计导出清单</p>
          <p class="text-2xl font-bold text-slate-800 mt-1">{{ stats.totalCount }} 份</p>
        </div>
        <div class="bg-white rounded-xl p-4 border border-slate-100 data-card">
          <p class="text-xs text-slate-400">覆盖企业数量</p>
          <p class="text-2xl font-bold text-slate-800 mt-1">{{ stats.enterpriseCount }} 家</p>
        </div>
      </div>
    </div>

    <!-- ==================== TAB 2: Generate New Report ==================== -->
    <div v-show="activeTab === 'new'">
      <div class="bg-white rounded-xl p-6 border border-slate-100 data-card">
        <h3 class="text-sm font-semibold text-slate-700 mb-4">
          <i class="fa-solid fa-wand-magic-sparkles mr-2 text-blue-600"></i>生成新报告
        </h3>
        <div class="grid grid-cols-2 gap-4 mb-4">
          <div>
            <label class="text-xs text-slate-500 block mb-1">报告类型</label>
            <select v-model="genForm.type" class="select-field">
              <option v-for="t in options.types" :key="t">{{ t }}</option>
            </select>
          </div>
          <div>
            <label class="text-xs text-slate-500 block mb-1">产业链范围</label>
            <select v-model="genForm.scope" class="select-field">
              <option v-for="s in options.scopes" :key="s">{{ s }}</option>
            </select>
          </div>
          <div>
            <label class="text-xs text-slate-500 block mb-1">目标企业数量</label>
            <select v-model="genForm.count" class="select-field">
              <option v-for="c in options.counts" :key="c">{{ c }}</option>
            </select>
          </div>
          <div>
            <label class="text-xs text-slate-500 block mb-1">输出格式</label>
            <select v-model="genForm.format" class="select-field">
              <option v-for="f in options.formats" :key="f">{{ f }}</option>
            </select>
          </div>
        </div>
        <button
          class="btn-primary w-full justify-center text-xs"
          :disabled="generating"
          @click="handleGenerate"
        >
          <i v-if="generating" class="fa-solid fa-spinner fa-spin"></i>
          <i v-else class="fa-solid fa-wand-magic-sparkles"></i>
          {{ generating ? '生成中...' : '立即生成报告' }}
        </button>
      </div>
    </div>

    <!-- ==================== TAB 3: Templates ==================== -->
    <div v-show="activeTab === 'templates'">
      <div class="grid grid-cols-3 gap-4">
        <div
          v-for="tmpl in templates"
          :key="tmpl.id"
          class="bg-white rounded-xl p-5 border border-slate-100 data-card cursor-pointer hover:shadow-md text-center"
          @click="useTemplate(tmpl)"
        >
          <div class="w-14 h-14 rounded-2xl flex items-center justify-center mx-auto mb-3" :class="tmpl.iconBg">
            <i :class="[tmpl.icon, tmpl.iconColor].join(' ') + ' text-2xl'"></i>
          </div>
          <h4 class="text-sm font-medium text-slate-700">{{ tmpl.name }}</h4>
          <p class="text-xs text-slate-400 mt-1">{{ tmpl.desc }}</p>
          <p class="text-xs text-slate-400">{{ tmpl.subDesc }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useEventBus } from '@/stores/eventBus'
import { reportsService } from '@/services/reports.service'

const eventBus = useEventBus()

// ---- State ----
const activeTab = ref('saved')
const reports = ref([])
const templates = ref([])
const stats = reactive({ monthCount: 0, totalCount: 0, enterpriseCount: 0 })
const generating = ref(false)

const tabs = [
  { key: 'saved', label: '已生成报告', icon: 'fa-regular fa-folder-open' },
  { key: 'new', label: '生成新报告', icon: 'fa-solid fa-plus-circle' },
  { key: 'templates', label: '报告模板', icon: 'fa-regular fa-copy' }
]

const genForm = reactive({
  type: '招商推荐清单',
  scope: '智能制造（全部节点）',
  count: 'Top 10',
  format: 'PDF'
})

const options = reactive({
  types: [],
  scopes: [],
  counts: [],
  formats: []
})

// ---- Init ----
onMounted(async () => {
  await loadData()
})

async function loadData() {
  try {
    const [reportList, statsData, templateList, opts] = await Promise.all([
      reportsService.getReports(),
      reportsService.getStats(),
      reportsService.getTemplates(),
      Promise.resolve(reportsService.getReportOptions())
    ])
    reports.value = reportList
    Object.assign(stats, statsData)
    templates.value = templateList
    Object.assign(options, opts)
  } catch (e) {
    console.error('Failed to load reports data:', e)
  }
}

// ---- Actions ----
function switchTab(key) {
  activeTab.value = key
}

function previewReport(report) {
  eventBus.openModal(report.name, `
    <div class="py-4">
      <div class="bg-slate-50 rounded-lg p-6 text-center">
        <i class="fa-regular fa-file-pdf text-slate-300 text-5xl mb-3"></i>
        <p class="text-sm text-slate-600">报告预览</p>
        <p class="text-xs text-slate-400 mt-2">包含 ${report.pages || 12} 页详细分析内容</p>
        <div class="flex gap-2 justify-center mt-4">
          <button class="btn-primary text-xs" id="modalDownloadBtn">
            <i class="fa-solid fa-download"></i> 下载
          </button>
          <button class="btn-outline text-xs" id="modalPrintBtn">
            <i class="fa-regular fa-file-pdf"></i> 打印
          </button>
        </div>
      </div>
    </div>
  `)
  // Bind buttons after DOM update
  setTimeout(() => {
    const dlBtn = document.getElementById('modalDownloadBtn')
    if (dlBtn) {
      dlBtn.onclick = () => {
        eventBus.closeModal()
        downloadReport(report)
      }
    }
  }, 100)
}

function downloadReport(report) {
  eventBus.openModal('下载报告', `
    <div class="text-center py-4">
      <i class="fa-solid fa-circle-check text-emerald-500 text-4xl mb-3"></i>
      <p class="text-sm text-slate-600">"${report.name}" 正在下载...</p>
    </div>
  `)
}

async function handleExportAll(type) {
  eventBus.openModal('批量导出', `
    <div class="text-center py-4">
      <i class="fa-solid fa-spinner fa-spin text-blue-500 text-3xl mb-3"></i>
      <p class="text-sm text-slate-600">正在打包导出所有 ${type.toUpperCase()} 报告...</p>
      <p class="text-xs text-slate-400 mt-1">3 份报告，共 45 页</p>
    </div>
  `)
  try {
    await reportsService.exportAll(type)
    eventBus.openModal('导出完成', `
      <div class="text-center py-4">
        <i class="fa-solid fa-circle-check text-emerald-500 text-4xl mb-3"></i>
        <p class="text-sm text-slate-600">全部报告已导出为 ${type.toUpperCase()} 格式</p>
        <p class="text-xs text-slate-400 mt-1">文件已保存至下载目录</p>
      </div>
    `)
  } catch (e) {
    console.error('[Reports] Operation failed:', e)
  }
}

async function handleGenerate() {
  generating.value = true
  try {
    const result = await reportsService.generateReport({
      type: genForm.type,
      scope: genForm.scope,
      count: genForm.count,
      format: genForm.format
    })
    eventBus.openModal('报告生成完成', `
      <div class="text-center py-4">
        <i class="fa-solid fa-circle-check text-emerald-500 text-4xl mb-3"></i>
        <p class="text-sm text-slate-600">"${result.name}" 已生成成功！</p>
        <p class="text-xs text-slate-400 mt-1">共 ${result.pages} 页，可在"已生成报告"中查看</p>
      </div>
    `)
    // Refresh the list and switch tab
    await loadData()
    activeTab.value = 'saved'
  } catch (e) {
    console.error('[Reports] Operation failed:', e)
  } finally {
    generating.value = false
  }
}

async function handleDeleteReport(report) {
  if (!confirm(`确认删除报告"${report.name}"？`)) return
  try {
    await reportsService.deleteReport(report.id)
    reports.value = reports.value.filter(r => r.id !== report.id)
  } catch (e) {
    console.error('[Reports] Operation failed:', e)
  }
}

function useTemplate(tmpl) {
  eventBus.openModal('使用模板', `
    <div class="py-4">
      <p class="text-sm text-slate-600">基于 "${tmpl.name}" 模板快速生成报告</p>
      <div class="mt-4 space-y-2">
        <select class="select-field" id="templateChainSelect">
          <option>智能制造产业链</option>
          <option>新能源汽车产业链</option>
          <option>集成电路产业链</option>
        </select>
        <button class="btn-primary w-full justify-center mt-3" id="templateGenerateBtn">
          <i class="fa-solid fa-wand-magic-sparkles"></i> 基于模板生成
        </button>
      </div>
    </div>
  `)
  // Bind the generate button
  setTimeout(() => {
    const btn = document.getElementById('templateGenerateBtn')
    if (btn) {
      btn.onclick = async () => {
        eventBus.closeModal()
        eventBus.openModal('生成中', `
          <div class="text-center py-4">
            <i class="fa-solid fa-spinner fa-spin text-blue-500 text-3xl mb-3"></i>
            <p class="text-sm text-slate-600">正在基于模板生成...</p>
          </div>
        `)
        try {
          await reportsService.generateFromTemplate(tmpl.name, '智能制造产业链')
          eventBus.openModal('生成完成', `
            <div class="text-center py-4">
              <i class="fa-solid fa-circle-check text-emerald-500 text-4xl mb-3"></i>
              <p class="text-sm text-slate-600">基于"${tmpl.name}"的报告已生成</p>
              <p class="text-xs text-slate-400 mt-1">可在"已生成报告"中查看</p>
            </div>
          `)
          await loadData()
          activeTab.value = 'saved'
        } catch (e) {
          console.error('[Reports] Operation failed:', e)
        }
      }
    }
  }, 100)
}
</script>

<style scoped>
/* Tab Button */
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

/* Shared */
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
.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
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

.data-card {
  transition: all 0.2s;
}
.data-card:hover {
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.08);
}
</style>
