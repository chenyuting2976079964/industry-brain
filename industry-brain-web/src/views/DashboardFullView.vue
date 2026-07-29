<template>
  <div class="dash-full" @click.self="$emit('close')">
    <!-- Top Stats Bar -->
    <div class="dash-topbar">
      <div class="dash-topbar-left">
        <img src="/logo_white.png" alt="" class="dash-logo" />
        <span class="dash-title">产业大脑 · 全景大屏</span>
      </div>
      <div class="dash-topbar-right">
        <span class="dash-clock">{{ timeDisplay }}</span>
        <span class="dash-status-dot"></span>
        <span>实时数据</span>
      </div>
    </div>

    <!-- Grid Layout -->
    <div class="dash-grid">
      <!-- Row 1: KPI -->
      <div class="dash-card dash-card-kpi">
        <div class="dash-kpi-label">专利总量</div>
        <div class="dash-kpi-value">{{ fmt(kpi.totalPatents) }}</div>
        <div class="dash-kpi-sub">含高价值 {{ fmt(kpi.highValuePatents) }} 件</div>
      </div>
      <div class="dash-card dash-card-kpi">
        <div class="dash-kpi-label">覆盖企业</div>
        <div class="dash-kpi-value">{{ fmt(kpi.coveredEnterprises) }}</div>
        <div class="dash-kpi-sub">共 {{ fmt(kpi.chainNodeCount) }} 个节点</div>
      </div>
      <div class="dash-card dash-card-kpi">
        <div class="dash-kpi-label">弱链环节</div>
        <div class="dash-kpi-value text-red-400">{{ weakCount }}</div>
        <div class="dash-kpi-sub">共 {{ totalNodes }} 个节点</div>
      </div>

      <!-- Row 2: Chain Strength Distribution -->
      <div class="dash-card dash-card-wide">
        <div class="dash-card-title"><i class="fa-solid fa-sitemap mr-2"></i>产业链强弱分布</div>
        <div class="dash-chain-bars">
          <div v-for="s in chainStrength" :key="s.status" class="dash-chain-bar-row">
            <span>{{ s.label }}</span>
            <div class="dash-bar-track">
              <div class="dash-bar-fill" :style="{ width: s.percentage + '%', background: s.color }"></div>
            </div>
            <span>{{ s.count }}个</span>
          </div>
        </div>
      </div>

      <!-- Row 3: Enterprise Distribution -->
      <div class="dash-card">
        <div class="dash-card-title"><i class="fa-solid fa-chart-bar mr-2"></i>企业分布</div>
        <div class="dash-diag-grid">
          <div v-for="d in enterpriseDist" :key="d.key" class="dash-diag-item" :class="'bg-blue-500/10'">
            <div class="text-2xl font-bold text-blue-400">{{ fmt(d.count) }}</div>
            <div class="text-xs text-slate-400">{{ d.label }}</div>
          </div>
        </div>
      </div>

      <!-- Row 4: Patent TOP5 -->
      <div class="dash-card">
        <div class="dash-card-title"><i class="fa-solid fa-trophy mr-2 text-amber-400"></i>高价值专利 TOP5</div>
        <div class="space-y-2">
          <div v-for="p in patentTop5" :key="p.patentId" class="dash-alert bg-slate-800/50 border-l-2 border-amber-500 p-2 text-xs flex justify-between">
            <span><span class="font-medium text-amber-300">#{{ p.rank }}</span> {{ p.patentName }}</span>
            <span class="text-slate-500">{{ p.applicant }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Map area -->
    <div class="dash-map-area">
      <img src="/photo/马鞍山市地图（暗色版）.png" alt="" class="dash-map-img" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { getKpi, getEnterpriseDist, getPatentTop5, getChainStrength } from '@/services/dashboardfullview.service'

const REGION = '340500'

// ─── Clock ───
const now = ref(new Date())
const timeDisplay = computed(() => {
  const y = now.value.getFullYear()
  const mo = String(now.value.getMonth() + 1).padStart(2, '0')
  const d = String(now.value.getDate()).padStart(2, '0')
  const h = String(now.value.getHours()).padStart(2, '0')
  const mi = String(now.value.getMinutes()).padStart(2, '0')
  const s = String(now.value.getSeconds()).padStart(2, '0')
  return `${y}-${mo}-${d} ${h}:${mi}:${s}`
})

// ─── API Data ───
const kpi = ref({ totalPatents: 0, highValuePatents: 0, coveredEnterprises: 0, chainNodeCount: 0 })
const enterpriseDist = ref([])
const chainStrength = ref([])
const patentTop5 = ref([])
const loading = ref(true)

function fmt(n) {
  if (n == null) return '0'
  return Number(n).toLocaleString('zh-CN')
}

const weakCount = computed(() => {
  const w = chainStrength.value.find(s => s.status === '弱链')
  return w ? w.count : 0
})
const totalNodes = computed(() => {
  return chainStrength.value.reduce((sum, s) => sum + s.count, 0)
})

onMounted(async () => {
  try {
    const [kpiData, distData, chainData, top5Data, markerData] = await Promise.all([
      getKpi(REGION),
      getEnterpriseDist(REGION),
      getChainStrength(REGION),
      getPatentTop5(REGION)
    ])
    kpi.value = kpiData
    enterpriseDist.value = distData
    chainStrength.value = chainData
    patentTop5.value = top5Data
    mapMarkers.value = markerData
  } catch (e) {
    console.error('大屏数据加载失败:', e)
  } finally {
    loading.value = false
  }
})

let timer = null
onMounted(() => {
  timer = setInterval(() => { now.value = new Date() }, 1000)
})
onUnmounted(() => {
  clearInterval(timer)
})
</script>

<style scoped>
.dash-full {
  min-height: 100vh;
  background: linear-gradient(135deg, #0f1e42 0%, #1e3a8a 40%, #1e40af 70%, #0f1e42 100%);
  color: #e2e8f0;
  padding: 0;
  position: relative;
  overflow: auto;
}

/* Topbar */
.dash-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 48px;
  background: rgba(0, 0, 0, 0.2);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}
.dash-topbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.dash-logo {
  width: 24px;
  height: 24px;
  object-fit: contain;
}
.dash-title {
  font-size: 14px;
  font-weight: 600;
  color: #bfdbfe;
}
.dash-topbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: #94a3b8;
}
.dash-clock {
  font-variant-numeric: tabular-nums;
}
.dash-status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #10b981;
  animation: pulse 2s infinite;
}

/* Grid */
.dash-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  padding: 20px;
}

.dash-card {
  background: rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  padding: 16px;
}

.dash-card-wide {
  grid-column: span 2;
}

.dash-card-title {
  font-size: 13px;
  font-weight: 600;
  color: #bfdbfe;
  margin-bottom: 12px;
}

/* KPI */
.dash-card-kpi {
  text-align: center;
}
.dash-kpi-label {
  font-size: 11px;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 1px;
}
.dash-kpi-value {
  font-size: 36px;
  font-weight: 700;
  color: #e2e8f0;
  margin: 4px 0;
  font-variant-numeric: tabular-nums;
}
.dash-kpi-sub {
  font-size: 11px;
  color: #64748b;
}

/* Chain bars */
.dash-chain-bars {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.dash-chain-bar-row {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
}
.dash-chain-bar-row span:first-child {
  width: 40px;
  color: #94a3b8;
}
.dash-chain-bar-row span:last-child {
  width: 50px;
  text-align: right;
  color: #cbd5e1;
}
.dash-bar-track {
  flex: 1;
  height: 10px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 5px;
  overflow: hidden;
}
.dash-bar-fill {
  height: 100%;
  border-radius: 5px;
  transition: width 1s ease;
}

/* Diagnosis */
.dash-diag-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}
.dash-diag-item {
  text-align: center;
  padding: 10px 6px;
  border-radius: 8px;
}

/* Alert */
.dash-alert {
  border-radius: 6px;
}

/* Map */
.dash-map-area {
  position: absolute;
  right: 20px;
  bottom: 20px;
  width: 400px;
  opacity: 0.85;
}
.dash-map-img {
  width: 100%;
  object-fit: contain;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}
</style>
