<template>
  <div class="dash-full" :class="{ 'theme-light': !isDark }">
    <transition name="fade">
      <div v-if="showEscHint" class="esc-hint">按 ESC 退出大屏</div>
    </transition>
    <div class="bg-decoration">
      <div class="bg-mountains"></div>
      <div class="bg-grid"></div>
      <div class="bg-glow bg-glow-1"></div>
      <div class="bg-glow bg-glow-2"></div>
    </div>

    <!-- 第一块：顶部标题栏 -->
    <div class="dash-topbar">
      <div class="dash-topbar-left">
        <span class="dash-clock">{{ timeDisplay }}</span>
      </div>
      <div class="dash-topbar-center">
        <img :src="isDark ? '/logo_white.png' : '/logo.png'" alt="" class="dash-logo" />
        <span class="dash-title">{{ regionName }} · 产业大脑</span>
      </div>
      <div class="dash-topbar-right">
        <span class="dash-location"><i class="fa-solid fa-location-dot"></i> {{ regionName }}</span>
        <button class="theme-toggle" @click="toggleTheme">
          <i :class="isDark ? 'fa-solid fa-sun' : 'fa-solid fa-moon'"></i>
          <span>{{ isDark ? '亮色' : '暗色' }}</span>
        </button>
        <span class="dash-status-dot"></span>
        <span>实时数据</span>
      </div>
    </div>

    <!-- 第二块：中间主体（左右面板 + 地图） -->
    <div class="dash-body">
      <div class="dash-left">
        <div class="panel">
          <div class="panel-title"><i class="fa-solid fa-building mr-2"></i>本地企业总体分布</div>
          <div class="local-ent-grid">
            <div class="le-item">
              <div class="le-val blue">{{ fmt(localStats.totalLocal) }}</div>
              <div class="le-label">企业总数</div>
            </div>
            <div class="le-item">
              <div class="le-val gold">{{ fmt(localStats.leadingEnterprise) }}</div>
              <div class="le-label">龙头企业</div>
            </div>
            <div class="le-item">
              <div class="le-val green">{{ fmt(localStats.highTechEnterprise) }}</div>
              <div class="le-label">高新技术企业</div>
            </div>
            <div class="le-item">
              <div class="le-val cyan">{{ fmt(localStats.techSME) }}</div>
              <div class="le-label">科技型中小企业</div>
            </div>
            <div class="le-item le-full">
              <div class="le-val purple">{{ fmt(localStats.regularEnterprise) }}</div>
              <div class="le-label">普通企业</div>
            </div>
          </div>
        </div>
        <div class="panel panel-tech">
          <div class="panel-title"><i class="fa-solid fa-microchip mr-2"></i>技术领域企业分布</div>
          <div class="tech-list">
            <div v-for="t in techDist.slice(0, 6)" :key="t.fieldName" class="tech-row">
              <span class="tech-name">{{ t.fieldName }}</span>
              <div class="tech-track"><div class="tech-fill" :style="{ width: t.percentage+'%' }"></div></div>
              <span class="tech-num">{{ t.enterpriseCount }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="dash-center">
        <img :src="isDark ? `/${regionName}地图（暗色版）.png` : `/${regionName}地图.png`" class="dash-map-img" />
      </div>

      <div class="dash-right">
        <div class="panel">
          <div class="panel-title"><i class="fa-solid fa-trophy mr-2 text-amber-400"></i>高价值专利 TOP5</div>
          <div class="patent-list">
            <div v-for="p in patentTop5" :key="p.patentId" class="patent-row">
              <span class="patent-rank" :class="'rank-' + p.rank">{{ p.rank }}</span>
              <span class="patent-name">{{ p.patentName }}</span>
            </div>
          </div>
        </div>
        <div class="panel panel-chart">
          <div class="panel-title"><i class="fa-solid fa-chart-pie mr-2"></i>产业链强弱分布</div>
          <div ref="chartRef" class="donut-chart"></div>
        </div>
      </div>
    </div>

    <!-- 第三块：底部KPI条 -->
    <div class="dash-kpi-divider"></div>
    <div class="dash-kpi-bar">
      <div class="kpi-item">
        <div class="kpi-item-icon blue"><i class="fa-regular fa-file-lines"></i></div>
        <div class="kpi-item-info">
          <div class="kpi-item-val">{{ fmt(kpi.totalPatents) }}</div>
          <div class="kpi-item-label">专利总数</div>
        </div>
      </div>
      <div class="kpi-item">
        <div class="kpi-item-icon gold"><i class="fa-solid fa-gem"></i></div>
        <div class="kpi-item-info">
          <div class="kpi-item-val">{{ fmt(kpi.highValuePatents) }}</div>
          <div class="kpi-item-label">高价值专利数</div>
        </div>
      </div>
      <div class="kpi-item">
        <div class="kpi-item-icon green"><i class="fa-solid fa-building"></i></div>
        <div class="kpi-item-info">
          <div class="kpi-item-val">{{ fmt(kpi.coveredEnterprises) }}</div>
          <div class="kpi-item-label">覆盖企业数</div>
        </div>
      </div>
      <div class="kpi-item">
        <div class="kpi-item-icon cyan"><i class="fa-solid fa-circle-nodes"></i></div>
        <div class="kpi-item-info">
          <div class="kpi-item-val">{{ fmt(kpi.chainNodeCount) }}</div>
          <div class="kpi-item-label">产业链节点数</div>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getKpi, getEnterpriseDist, getPatentTop5, getChainStrength, getTechDist, getLocalEnterpriseStats } from '@/services/dashboardfullview.service'

const REGION_CODE = '340500'
const REGION_NAMES = { '340500': '马鞍山市', '340100': '合肥市', '340200': '芜湖市', '320100': '南京市', '320500': '苏州市', '330100': '杭州市' }
const regionName = REGION_NAMES[REGION_CODE] || REGION_CODE
const isDark = ref(true)
const showEscHint = ref(true)
function toggleTheme() { isDark.value = !isDark.value; setTimeout(renderDonut, 50) }

const timeDisplay = ref('')
function updateClock() {
  const d = new Date()
  const p = n => String(n).padStart(2, '0')
  timeDisplay.value = `${d.getFullYear()}-${p(d.getMonth()+1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}
updateClock()

function fmt(n) { return n == null ? '0' : Number(n).toLocaleString('zh-CN') }

const kpi = ref({ totalPatents: 0, highValuePatents: 0, coveredEnterprises: 0, chainNodeCount: 0 })
const localStats = ref({ totalLocal: 0, leadingEnterprise: 0, highTechEnterprise: 0, techSME: 0, regularEnterprise: 0 })
const chainStrength = ref([])
const patentTop5 = ref([])
const techDist = ref([])
const chartRef = ref(null)

onMounted(() => {
  timer = setInterval(updateClock, 1000)
  setTimeout(() => { showEscHint.value = false }, 4000)
  loadData()
})

async function loadData() {
  try {
    const [kpiData, chainData, top5Data, techData, localData] = await Promise.all([
      getKpi(REGION_CODE),
      getChainStrength(REGION_CODE),
      getPatentTop5(REGION_CODE),
      getTechDist(REGION_CODE),
      getLocalEnterpriseStats(REGION_CODE)
    ])
    kpi.value = kpiData
    chainStrength.value = chainData || []
    patentTop5.value = top5Data || []
    techDist.value = techData || []

    if (localData) {
      localStats.value = {
        totalLocal: localData.totalLocal || 0,
        leadingEnterprise: localData.leadingEnterprise || 0,
        highTechEnterprise: localData.highTechEnterprise || 0,
        techSME: localData.techSME || 0,
        regularEnterprise: localData.regularEnterprise || 0
      }
    }

    await nextTick()
    renderDonut()
  } catch (e) {
    console.error(e)
  }
}

function renderDonut() {
  if (!chartRef.value) return
  const chart = echarts.init(chartRef.value)
  const data = chainStrength.value.map(s => ({
    value: s.count,
    name: s.label,
    itemStyle: { color: s.color }
  }))
  const total = data.reduce((sum, d) => sum + d.value, 0)
  const light = !isDark.value
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}个 ({d}%)' },
    series: [{
      type: 'pie',
      radius: ['38%', '72%'],
      center: ['50%', '52%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 4, borderColor: light ? '#fff' : 'rgba(10,18,45,0.8)', borderWidth: 3 },
      label: {
        show: true,
        position: 'outside',
        formatter: '{b} {d}%',
        color: light ? '#475569' : '#94a3b8',
        fontSize: 11,
        lineHeight: 18
      },
      labelLine: { length: 15, length2: 20, lineStyle: { color: light ? '#cbd5e1' : '#4a5568' } },
      emphasis: { label: { fontSize: 14, fontWeight: 'bold' } },
      data
    }],
    graphic: [{
      type: 'text',
      left: 'center',
      top: '42%',
      style: {
        text: total,
        textAlign: 'center',
        fill: light ? '#1e293b' : '#f1f5f9',
        fontSize: 26,
        fontWeight: 700
      }
    }, {
      type: 'text',
      left: 'center',
      top: '52%',
      style: {
        text: '总节点数',
        textAlign: 'center',
        fill: light ? '#64748b' : '#94a3b8',
        fontSize: 11
      }
    }]
  })
}

let timer = null
onUnmounted(() => { if (timer) { clearInterval(timer); timer = null } })
</script>

<style scoped>
.dash-full{height:100%;width:100%;background:linear-gradient(160deg,#060b1f 0%,#0b1a4a 30%,#0c2062 60%,#060b1f 100%);color:#cbd5e1;font-family:'Microsoft YaHei','PingFang SC',sans-serif;overflow:hidden;position:relative;display:flex;flex-direction:column;max-width:100vw}
.bg-decoration{position:absolute;inset:0;pointer-events:none;z-index:0;overflow:hidden}
.bg-mountains{position:absolute;bottom:-2px;left:0;right:0;height:35%;z-index:0;opacity:.7;background:linear-gradient(200deg,transparent 52%,rgba(42,72,140,.4) 53%,rgba(30,55,115,.45) 63%,transparent 64%),linear-gradient(188deg,transparent 57%,rgba(38,68,132,.45) 58%,rgba(28,52,108,.5) 69%,transparent 70%),linear-gradient(182deg,transparent 63%,rgba(32,60,120,.5) 64%,rgba(24,46,98,.55) 75%,transparent 76%),linear-gradient(186deg,transparent 70%,rgba(22,48,105,.55) 71%,rgba(16,36,82,.6) 84%,rgba(10,22,58,.5) 94%,transparent 95%);mask-image:linear-gradient(to top,rgba(0,0,0,.6) 0%,rgba(0,0,0,.2) 35%,transparent 70%);-webkit-mask-image:linear-gradient(to top,rgba(0,0,0,.6) 0%,rgba(0,0,0,.2) 35%,transparent 70%)}
.bg-grid{position:absolute;inset:0;z-index:0;opacity:.4;background-image:linear-gradient(rgba(79,140,255,.05) 1px,transparent 1px),linear-gradient(90deg,rgba(79,140,255,.05) 1px,transparent 1px);background-size:80px 80px}
.bg-glow{position:absolute;border-radius:50%;filter:blur(160px);z-index:0}
.bg-glow-1{width:500px;height:500px;opacity:.1;background:radial-gradient(circle,#3b82f6 0%,transparent 70%);top:-200px;right:-100px}
.bg-glow-2{width:400px;height:400px;opacity:.08;background:radial-gradient(circle,#7c3aed 0%,transparent 70%);bottom:-150px;left:-80px}

.dash-topbar{display:flex;justify-content:space-between;align-items:center;padding:0 28px;height:56px;min-height:56px;background:rgba(8,20,60,.95);border-bottom:1px solid rgba(99,144,240,.1);backdrop-filter:blur(20px);position:relative;z-index:10;flex-shrink:0}
.dash-topbar-left{display:flex;align-items:center;gap:10px;font-size:13px;color:#64748b;flex:1}
.dash-topbar-center{display:flex;align-items:center;gap:14px;position:absolute;left:50%;transform:translateX(-50%)}
.dash-logo{width:32px;height:32px;object-fit:contain}
.dash-title{font-size:22px;font-weight:700;background:linear-gradient(90deg,#60a5fa,#c4b5fd,#60a5fa);-webkit-background-clip:text;-webkit-text-fill-color:transparent;letter-spacing:5px;white-space:nowrap}
.dash-topbar-right{display:flex;align-items:center;gap:12px;font-size:13px;color:#64748b;flex:1;justify-content:flex-end}
.dash-location{color:#60a5fa;font-size:13px;display:flex;align-items:center;gap:5px}
.dash-status-dot{width:7px;height:7px;border-radius:50%;background:#22c55e;box-shadow:0 0 8px rgba(34,197,94,.5);animation:pulse 2s infinite}
@keyframes pulse{0%,100%{opacity:1}50%{opacity:.4}}

/* 第二块：主体区域 */
.dash-body{flex:1;min-height:0;display:flex;gap:16px;padding:14px 16px 8px;position:relative;z-index:1}
.dash-left{width:310px;display:flex;flex-direction:column;gap:12px;overflow-y:auto;overflow-x:hidden;scrollbar-width:none;flex-shrink:0}
.dash-right{width:310px;display:flex;flex-direction:column;gap:12px;overflow-y:auto;overflow-x:hidden;scrollbar-width:none;flex-shrink:0}
.dash-left::-webkit-scrollbar,.dash-right::-webkit-scrollbar{display:none}
.dash-center{flex:1;display:flex;align-items:flex-start;justify-content:center;border-radius:12px;overflow:hidden;background:rgba(10,18,45,.2)}

.panel{background:rgba(20,36,80,.5);border:1px solid rgba(78,128,240,.1);border-radius:12px;padding:16px;backdrop-filter:blur(14px);box-shadow:0 4px 20px rgba(0,0,0,.12)}
.panel-title{font-size:15px;font-weight:600;color:#93c5fd;margin-bottom:16px;padding-bottom:10px;border-bottom:1px solid rgba(78,128,240,.1);letter-spacing:1.5px}
.panel-tech{flex:1.2;min-height:0;overflow-y:auto}
.panel-chart{flex:0.9;min-height:0;display:flex;flex-direction:column;justify-content:center}

/* 本地企业总体分布 */
.local-ent-grid{display:grid;grid-template-columns:1fr 1fr;gap:10px}
.le-item.le-full{grid-column:span 2;padding:14px 10px}
.le-item{text-align:center;background:rgba(18,32,72,.6);border:1px solid rgba(78,128,240,.08);border-radius:10px;padding:16px 10px;transition:border-color .2s}
.le-item:hover{border-color:rgba(78,128,240,.2)}
.le-val{font-size:26px;font-weight:700;line-height:1.2}
.le-val.blue{color:#60a5fa}
.le-val.gold{color:#f59e0b}
.le-val.green{color:#34d399}
.le-val.cyan{color:#22d3ee}
.le-val.purple{color:#a78bfa}
.le-label{font-size:11px;color:#8ba0c0;margin-top:6px;letter-spacing:.5px}

/* 技术领域分布 */
.tech-list{display:flex;flex-direction:column;gap:12px}
.tech-row{display:flex;align-items:center;gap:10px}
.tech-name{font-size:12px;color:#94a3b8;width:88px;text-align:right;flex-shrink:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.tech-track{flex:1;height:10px;background:rgba(255,255,255,.06);border-radius:5px;overflow:hidden}
.tech-fill{height:100%;border-radius:5px;background:linear-gradient(90deg,#2563eb,#60a5fa,#93c5fd);transition:width .8s ease}
.tech-num{font-size:12px;color:#cbd5e1;font-weight:600;width:36px;text-align:right}

/* 专利TOP5 */
.patent-list{display:flex;flex-direction:column;gap:8px}
.patent-row{display:flex;align-items:center;gap:10px;padding:8px 0;border-bottom:1px solid rgba(255,255,255,.04)}
.patent-row:last-child{border-bottom:none}
.patent-rank{width:22px;height:22px;border-radius:5px;display:flex;align-items:center;justify-content:center;font-size:11px;font-weight:700;color:#fff;flex-shrink:0}
.patent-rank.rank-1{background:linear-gradient(135deg,#f59e0b,#d97706)}
.patent-rank.rank-2{background:linear-gradient(135deg,#94a3b8,#64748b)}
.patent-rank.rank-3{background:linear-gradient(135deg,#b45309,#92400e)}
.patent-rank.rank-4,.patent-rank.rank-5{background:#2a3550;color:#8899b4}
.patent-name{font-size:12px;color:#c8d2e2;flex:1;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.patent-score{font-size:11px;color:#5c6e8a;flex-shrink:0;font-weight:500}

/* 环形图 */
.donut-chart{width:100%;height:270px;flex-shrink:0}

/* 中间地图 */
.dash-map-img{max-width:96%;max-height:96%;object-fit:contain;opacity:.94;margin-top:12px}

/* ESC 提示 */
.esc-hint{position:absolute;bottom:120px;left:50%;transform:translateX(-50%);z-index:20;background:rgba(0,0,0,.7);color:rgba(255,255,255,.7);padding:8px 20px;border-radius:20px;font-size:13px;letter-spacing:2px;pointer-events:none}
.fade-leave-active{transition:opacity 1s}.fade-leave-to{opacity:0}

/* 第三块：底部KPI条 */
.dash-kpi-divider{height:1px;margin:4px 16px 0;background:linear-gradient(90deg,transparent,rgba(255,255,255,.15),rgba(255,255,255,.25),rgba(255,255,255,.15),transparent);flex-shrink:0}
.dash-kpi-bar{height:100px;display:flex;gap:18px;padding:14px 16px 16px;flex-shrink:0;position:relative;z-index:10}
.kpi-item{flex:1;background:rgba(20,36,80,.45);border:1px solid rgba(78,128,240,.1);border-radius:14px;padding:0 32px;display:flex;align-items:center;gap:20px;backdrop-filter:blur(14px);transition:border-color .2s}
.kpi-item:hover{border-color:rgba(78,128,240,.2)}
.kpi-item-icon{width:52px;height:52px;border-radius:12px;display:flex;align-items:center;justify-content:center;font-size:24px;color:#fff;flex-shrink:0}
.kpi-item-icon.blue{background:linear-gradient(135deg,#1d4ed8,#3b82f6);box-shadow:0 4px 16px rgba(37,99,235,.35)}
.kpi-item-icon.gold{background:linear-gradient(135deg,#b45309,#f59e0b);box-shadow:0 4px 16px rgba(245,158,11,.35)}
.kpi-item-icon.green{background:linear-gradient(135deg,#047857,#10b981);box-shadow:0 4px 16px rgba(16,185,129,.35)}
.kpi-item-icon.cyan{background:linear-gradient(135deg,#0e7490,#06b6d4);box-shadow:0 4px 16px rgba(6,182,212,.35)}
.kpi-item-info{display:flex;flex-direction:column}
.kpi-item-val{font-size:26px;font-weight:700;color:#f1f5f9;line-height:1.1}
.kpi-item-label{font-size:13px;color:#94a3b8;margin-top:4px;letter-spacing:1px}

.theme-light .dash-kpi-divider{background:linear-gradient(90deg,transparent,rgba(0,0,0,.06),rgba(0,0,0,.1),rgba(0,0,0,.06),transparent)}
.theme-light .dash-kpi-bar .kpi-item{background:rgba(255,255,255,.75);border-color:rgba(59,130,246,.06)}
.theme-light .kpi-item-val{color:#0f172a}
.theme-light .kpi-item-label{color:#64748b}

.theme-toggle{display:flex;align-items:center;gap:5px;background:rgba(255,255,255,.06);border:1px solid rgba(255,255,255,.08);border-radius:14px;padding:3px 12px;font-size:12px;color:#6b7d99;cursor:pointer}
.theme-toggle:hover{background:rgba(255,255,255,.1);color:#bcc8dd}

.theme-light.dash-full{background:linear-gradient(160deg,#eef2fb 0%,#e4eafa 30%,#eef2fb 60%,#e4eafa 100%);color:#334155}
.theme-light .dash-topbar{background:rgba(255,255,255,.9);border-bottom-color:rgba(59,130,246,.08)}
.theme-light .panel{background:rgba(255,255,255,.7);border-color:rgba(59,130,246,.06);box-shadow:0 4px 16px rgba(0,0,0,.03)}
.theme-light .panel-title{color:#1d4ed8;border-bottom-color:rgba(59,130,246,.1)}
.theme-light .dash-title{background:linear-gradient(90deg,#1d4ed8,#3b82f6,#1d4ed8);-webkit-background-clip:text;-webkit-text-fill-color:transparent}
.theme-light .dash-topbar-right,.theme-light .dash-topbar-left{color:#64748b}
.theme-light .dash-location{color:#2563eb}
.theme-light .theme-toggle{background:rgba(0,0,0,.03);border-color:rgba(0,0,0,.06);color:#64748b}
.theme-light .theme-toggle:hover{background:rgba(0,0,0,.06);color:#334155}
.theme-light .le-item{background:rgba(59,130,246,.05);border-color:rgba(59,130,246,.08)}
.theme-light .le-val.blue{color:#2563eb}
.theme-light .le-val.gold{color:#d97706}
.theme-light .le-val.green{color:#059669}
.theme-light .le-val.cyan{color:#0891b2}
.theme-light .le-val.purple{color:#7c3aed}
.theme-light .le-label{color:#64748b}
.theme-light .tech-name{color:#64748b}
.theme-light .tech-track{background:rgba(0,0,0,.06)}
.theme-light .tech-num{color:#475569}
.theme-light .patent-name{color:#1e293b}
.theme-light .patent-score{color:#64748b}
.theme-light .dash-center{background:rgba(59,130,246,.03)}
.theme-light .dash-map-img{opacity:1;filter:none}
.theme-light .bg-mountains{opacity:.5;background:linear-gradient(200deg,transparent 52%,rgba(59,130,246,.07) 53%,rgba(59,130,246,.09) 63%,transparent 64%),linear-gradient(188deg,transparent 57%,rgba(59,130,246,.09) 58%,rgba(59,130,246,.11) 69%,transparent 70%),linear-gradient(182deg,transparent 63%,rgba(59,130,246,.11) 64%,rgba(59,130,246,.13) 75%,transparent 76%),linear-gradient(186deg,transparent 70%,rgba(59,130,246,.12) 71%,rgba(59,130,246,.14) 84%,rgba(59,130,246,.07) 94%,transparent 95%)}
.theme-light .bg-grid{background-image:linear-gradient(rgba(59,130,246,.04) 1px,transparent 1px),linear-gradient(90deg,rgba(59,130,246,.04) 1px,transparent 1px);opacity:.3}
.theme-light .bg-glow-1{opacity:.05}.theme-light .bg-glow-2{opacity:.04}
</style>
