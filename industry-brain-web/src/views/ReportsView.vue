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
      <div></div>
    </div>

    <!-- ==================== Stats Cards ==================== -->
    <div class="grid grid-cols-3 gap-4 mb-4">
      <div class="bg-white rounded-xl p-4 border border-slate-100">
        <p class="text-xs text-slate-400">本月生成报告</p>
        <p class="text-2xl font-bold text-slate-800 mt-1">{{ stats.monthCount }} 份</p>
      </div>
      <div class="bg-white rounded-xl p-4 border border-slate-100">
        <p class="text-xs text-slate-400">累计导出清单</p>
        <p class="text-2xl font-bold text-slate-800 mt-1">{{ stats.totalCount }} 份</p>
      </div>
      <div class="bg-white rounded-xl p-4 border border-slate-100">
        <p class="text-xs text-slate-400">覆盖企业数量</p>
        <p class="text-2xl font-bold text-slate-800 mt-1">{{ stats.enterpriseCount }} 家</p>
      </div>
    </div>

    <!-- ==================== Report List ==================== -->
    <div v-if="reports.length === 0" class="bg-white rounded-xl p-10 border border-slate-100 text-center">
      <i class="fa-regular fa-folder-open text-slate-200 text-5xl block mb-3"></i>
      <p class="text-sm text-slate-400">暂无报告，请在右侧"生成新报告"面板创建</p>
    </div>
    <div v-else class="space-y-2">
      <div
        v-for="r in reports"
        :key="r.id"
        class="bg-white rounded-xl p-4 border border-slate-100 flex items-center justify-between"
      >
        <div class="flex items-center gap-3 flex-1 min-w-0">
          <div class="w-10 h-10 rounded-lg flex items-center justify-center"
            :class="r.status === 'completed' ? 'bg-emerald-50' : r.status === 'processing' ? 'bg-blue-50' : 'bg-red-50'"
          >
            <i :class="r.status === 'completed' ? 'fa-regular fa-file-excel text-emerald-500' : r.status === 'processing' ? 'fa-solid fa-spinner fa-spin text-blue-500' : 'fa-regular fa-circle-exclamation text-red-400'"></i>
          </div>
          <div class="flex-1 min-w-0">
            <p class="text-sm font-medium text-slate-700 truncate">{{ r.title || '招商推荐报告' }}</p>
            <p class="text-xs text-slate-400 mt-0.5">
              <span v-if="r.reportDate">生成时间: {{ formatDate(r.reportDate) }}</span>
              <span v-if="r.status === 'processing'">正在生成中...</span>
              <span v-if="r.status === 'failed'">生成失败</span>
              <span v-if="r.enterpriseCount" class="ml-2">· {{ r.enterpriseCount }} 家企业</span>
              <span v-if="r.nodeCount" class="ml-2">· {{ r.nodeCount }} 个节点</span>
            </p>
          </div>
        </div>
        <button
          v-if="r.status === 'completed'"
          class="btn-outline text-xs flex-shrink-0 ml-3"
          @click="downloadReport(r)"
        >
          <i class="fa-solid fa-download"></i>下载
        </button>
        <button
          v-if="r.status === 'completed' || r.status === 'failed'"
          class="btn-outline text-xs flex-shrink-0 ml-2 text-red-400"
          @click="handleDeleteReport(r)"
        >
          <i class="fa-regular fa-trash-can"></i>
        </button>
      </div>
    </div>

    <!-- ==================== Generate Panel ==================== -->
    <div class="bg-white rounded-xl p-6 border border-slate-100 mb-4">
      <h3 class="text-sm font-semibold text-slate-700 mb-4">
        <i class="fa-solid fa-wand-magic-sparkles mr-2 text-blue-600"></i>生成新报告
      </h3>
      <div class="flex items-end gap-4 flex-wrap">
        <div>
          <label class="text-xs text-slate-500 block mb-1">报告类型</label>
          <select v-model="genForm.type" class="select-field">
            <option value="investment_list">招商推荐清单</option>
            <option value="diagnosis">产业链诊断报告</option>
            <option value="enterprise_profile">企业画像报告</option>
          </select>
        </div>
        <div>
          <label class="text-xs text-slate-500 block mb-1">输出格式</label>
          <select v-model="genForm.format" class="select-field">
            <option value="Excel">Excel</option>
            <option value="PDF">PDF</option>
          </select>
        </div>
        <button
          class="btn-primary text-xs"
          :disabled="generating"
          @click="handleGenerate"
        >
          <i v-if="generating" class="fa-solid fa-spinner fa-spin"></i>
          <i v-else class="fa-solid fa-wand-magic-sparkles"></i>
          {{ generating ? '生成中...' : '立即生成报告' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { reportsService } from '@/services/reports.service'

// ---- State ----
const reports = ref([])
const stats = reactive({ monthCount: 0, totalCount: 0, enterpriseCount: 0 })
const generating = ref(false)

const genForm = reactive({
  type: 'investment_list',
  scope: '全部产业链',
  format: 'Excel'
})

// ---- Init ----
onMounted(async () => {
  await loadData()
})

async function loadData() {
  try {
    const [reportList, statsData] = await Promise.all([
      reportsService.getReports(),
      reportsService.getStats()
    ])
    // API返回 { reports, total, page, size } 或直接数组
    if (reportList && reportList.reports) {
      reports.value = reportList.reports
    } else if (Array.isArray(reportList)) {
      reports.value = reportList
    } else {
      reports.value = []
    }
    if (statsData) Object.assign(stats, statsData)
  } catch (e) {
    console.error('Failed to load reports data:', e)
  }
}

function formatDate(d) {
  if (!d) return ''
  const dt = new Date(d)
  const pad = n => String(n).padStart(2, '0')
  return `${dt.getFullYear()}-${pad(dt.getMonth()+1)}-${pad(dt.getDate())} ${pad(dt.getHours())}:${pad(dt.getMinutes())}`
}

// ---- Actions ----
async function downloadReport(report) {
  try {
    const url = await reportsService.downloadReport(report.id)
    if (url) window.open(url, '_blank')
    else alert('报告文件暂不可用')
  } catch (e) {
    alert('下载失败')
  }
}

async function handleGenerate() {
  generating.value = true
  try {
    await reportsService.generateReport({
      type: genForm.type,
      format: genForm.format
    })
    await loadData()
  } catch (e) {
    console.error('[Reports] Generation failed:', e)
  } finally {
    generating.value = false
  }
}

async function handleDeleteReport(report) {
  if (!confirm('确认删除该报告？')) return
  try {
    await reportsService.deleteReport(report.id)
    reports.value = reports.value.filter(r => r.id !== report.id)
  } catch (e) {
    console.error('[Reports] Operation failed:', e)
  }
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
