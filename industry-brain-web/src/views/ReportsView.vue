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
      <!-- 批量操作栏 -->
      <div v-if="selectedIds.length > 0" class="bg-blue-50 rounded-xl p-3 border border-blue-200 flex items-center justify-between mb-3">
        <span class="text-sm text-blue-700">
          <i class="fa-solid fa-check-circle mr-1"></i>
          已选择 <strong>{{ selectedIds.length }}</strong> 份报告
        </span>
        <div class="flex items-center gap-2">
          <button
            class="btn-outline text-xs"
            @click="clearSelection"
          >
            <i class="fa-regular fa-circle-xmark"></i>清空
          </button>
          <button
            class="btn-danger text-xs"
            @click="handleBatchDelete"
          >
            <i class="fa-regular fa-trash-can"></i>删除所选 ({{ selectedIds.length }})
          </button>
        </div>
      </div>

      <div class="flex items-center justify-between mb-2 px-1">
        <label class="flex items-center gap-2 text-xs text-slate-500 cursor-pointer">
          <input
            type="checkbox"
            :checked="isAllSelected"
            :indeterminate.prop="isIndeterminate"
            class="w-4 h-4 rounded border-slate-300 text-blue-600 focus:ring-blue-500"
            @change="toggleSelectAll"
          >
          <span>全选</span>
        </label>
        <span class="text-xs text-slate-400">共 {{ reports.length }} 份报告</span>
      </div>

      <div class="space-y-2">
        <div
          v-for="r in reports"
          :key="r.id"
          class="bg-white rounded-xl p-4 border flex items-center gap-4 report-item"
          :class="[
            String(r.id) === String(highlightId) ? 'border-blue-400 shadow-[0_0_12px_rgba(59,130,246,0.2)] highlight-pulse' : 'border-slate-100',
            selectedIds.includes(r.id) ? 'border-blue-400 bg-blue-50' : ''
          ]"
        >
          <!-- 多选框 -->
          <input
            type="checkbox"
            :checked="selectedIds.includes(r.id)"
            class="w-4 h-4 rounded border-slate-300 text-blue-600 focus:ring-blue-500 flex-shrink-0"
            @change="toggleSelect(r.id)"
          >

          <div class="w-10 h-10 rounded-lg flex items-center justify-center flex-shrink-0"
            :class="reportIconBg(r)"
          >
            <i :class="reportIcon(r)"></i>
          </div>
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2">
              <p class="text-sm font-medium text-slate-700 truncate">{{ r.title || '招商推荐报告' }}</p>
              <span v-if="String(r.id) === String(highlightId)" class="text-[10px] bg-blue-100 text-blue-600 px-1.5 py-0.5 rounded font-medium">新生成</span>
            </div>
            <p class="text-xs text-slate-400 mt-0.5">
              <span>{{ typeLabels[r.reportType] || r.reportType || '招商推荐清单' }}</span>
              <span class="mx-1.5">·</span>
              <span v-if="r.reportDate || r.createdAt">{{ formatDate(r.reportDate || r.createdAt) }}</span>
              <span v-else-if="isPending(r.status)">正在生成中...</span>
              <span v-if="r.status === 'failed'" class="text-red-400">生成失败</span>
              <span v-if="r.enterpriseCount" class="ml-1.5">· {{ r.enterpriseCount }} 家企业</span>
              <span v-if="r.format || r.outputFormat" class="ml-1.5">· {{ r.format || r.outputFormat }}</span>
            </p>
          </div>
          <button
            v-if="isDone(r.status)"
            class="btn-outline text-xs flex-shrink-0 ml-3"
            @click="downloadReport(r)"
          >
            <i class="fa-solid fa-download"></i>下载
          </button>
          <button
            v-if="isDone(r.status) || r.status === 'failed'"
            class="btn-outline text-xs flex-shrink-0 ml-2 text-red-400"
            @click="handleDeleteReport(r)"
          >
            <i class="fa-regular fa-trash-can"></i>
          </button>
        </div>
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
            <option value="Excel">Excel（XLSX）</option>
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
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { reportsService } from '@/services/reports.service'

// ---- State ----
const reports = ref([])
const stats = reactive({ monthCount: 0, totalCount: 0, enterpriseCount: 0 })
const generating = ref(false)
const highlightId = ref(null)
const selectedIds = ref([])
let pollTimer = null
let pollCount = 0

const genForm = reactive({
  type: 'investment_list',
  scope: '全部产业链',
  format: 'Excel'
})

// 报告类型中文映射
const typeLabels = {
  investment_list: '招商推荐清单',
  diagnosis: '产业链诊断报告',
  enterprise_profile: '企业画像报告',
  chain_supplement: '补链建议方案'
}

// ---- Init ----
onMounted(async () => {
  await loadData()

  // 检查是否有来自其他页面的新报告ID
  try {
    const ctx = JSON.parse(sessionStorage.getItem('navContext') || '{}')
    if (ctx.newReportId) {
      highlightId.value = ctx.newReportId
      sessionStorage.removeItem('navContext')
      // 开始轮询，等待报告生成完成
      startPolling()
    }
  } catch (e) { /* ignore */ }
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})

function startPolling() {
  if (pollTimer) clearInterval(pollTimer)
  pollCount = 0
  pollTimer = setInterval(async () => {
    await loadData()
    pollCount++
    const target = reports.value.find(r => String(r.id) === String(highlightId.value))
    // 目标已找到且完成 → 停止；未找到但还在轮询中 → 继续（报告可能在列表第一页之外）
    // 最长轮询 2 分钟（40 次 × 3s），避免网络异常时无限轮询
    if ((target && (isDone(target.status) || target.status === 'failed')) || pollCount >= 40) {
      clearInterval(pollTimer)
      pollTimer = null
    }
  }, 3000)
}

// 数据库 status: pending/processing/done/failed（CHECK约束）
function isDone(status) { return status === 'done' || status === 'completed' }
function isPending(status) { return status === 'pending' || status === 'processing' }

// ---- 多选功能 ----
const isAllSelected = computed(() => {
  const selectable = reports.value.filter(r => isDone(r.status) || r.status === 'failed')
  return selectable.length > 0 && selectable.every(r => selectedIds.value.includes(r.id))
})

const isIndeterminate = computed(() => {
  const selectable = reports.value.filter(r => isDone(r.status) || r.status === 'failed')
  const selectedCount = selectable.filter(r => selectedIds.value.includes(r.id)).length
  return selectedCount > 0 && selectedCount < selectable.length
})

function toggleSelectAll() {
  const selectable = reports.value.filter(r => isDone(r.status) || r.status === 'failed')
  if (isAllSelected.value) {
    // 取消全选
    selectedIds.value = selectedIds.value.filter(id => !selectable.some(r => r.id === id))
  } else {
    // 全选
    selectable.forEach(r => {
      if (!selectedIds.value.includes(r.id)) {
        selectedIds.value.push(r.id)
      }
    })
  }
}

function toggleSelect(id) {
  const idx = selectedIds.value.indexOf(id)
  if (idx >= 0) {
    selectedIds.value.splice(idx, 1)
  } else {
    selectedIds.value.push(id)
  }
}

function clearSelection() {
  selectedIds.value = []
}

async function handleBatchDelete() {
  if (selectedIds.value.length === 0) return
  const count = selectedIds.value.length
  if (!confirm(`确认删除选中的 ${count} 份报告？此操作不可恢复。`)) return

  try {
    // 并发删除
    await Promise.all(selectedIds.value.map(id => reportsService.deleteReport(id)))
    // 从列表中移除
    reports.value = reports.value.filter(r => !selectedIds.value.includes(r.id))
    // 清空选中
    selectedIds.value = []
    // 刷新统计
    await loadData()
  } catch (e) {
    console.error('[Reports] Batch delete failed:', e)
    alert('删除失败，请重试')
  }
}

// ---- 报告图标：按格式区分（PDF=红色文件图标，Excel=绿色表格图标） ----
function reportIcon(r) {
  if (isPending(r.status)) return 'fa-solid fa-spinner fa-spin text-blue-500'
  if (r.status === 'failed') return 'fa-regular fa-circle-exclamation text-red-400'
  const fmt = (r.format || r.outputFormat || '').toUpperCase()
  return fmt === 'PDF'
    ? 'fa-solid fa-file-pdf text-red-500'
    : 'fa-regular fa-file-excel text-emerald-500'
}

function reportIconBg(r) {
  if (isPending(r.status)) return 'bg-blue-50'
  if (r.status === 'failed') return 'bg-red-50'
  const fmt = (r.format || r.outputFormat || '').toUpperCase()
  return fmt === 'PDF' ? 'bg-red-50' : 'bg-emerald-50'
}

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
    if (statsData) {
      stats.monthCount = statsData.monthlyGenerated || statsData.monthCount || 0
      stats.totalCount = statsData.totalExported || statsData.totalCount || 0
      stats.enterpriseCount = statsData.coveredEnterprises || statsData.enterpriseCount || 0
    }
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
    const reportId = await reportsService.generateReport({
      type: genForm.type,
      format: genForm.format
    })
    // 高亮新报告，启动轮询等待生成完成（本页生成同样需要自动刷新）
    highlightId.value = reportId
    await loadData()
    startPolling()
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
.report-item { transition: all 0.3s ease; }
.highlight-pulse {
  animation: highlightPulse 2s ease-in-out infinite;
}
@keyframes highlightPulse {
  0%, 100% { box-shadow: 0 0 12px rgba(59,130,246,0.2); }
  50% { box-shadow: 0 0 20px rgba(59,130,246,0.4); }
}
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

.btn-danger {
  border: 1px solid #fecaca;
  background: #fef2f2;
  color: #dc2626;
  padding: 6px 14px;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.btn-danger:hover {
  background: #fee2e2;
  border-color: #f87171;
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
