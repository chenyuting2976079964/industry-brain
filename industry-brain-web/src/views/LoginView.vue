<template>
  <div class="login-page">
    <!-- 左侧品牌展示区 -->
    <div class="brand-panel">
      <div class="brand-bg">
        <div class="brand-grid"></div>
        <div class="brand-glow glow-1"></div>
        <div class="brand-glow glow-2"></div>
        <div class="brand-glow glow-3"></div>
      </div>

      <div class="brand-content">
        <div class="brand-logo">
          <img src="/logo_white.png" alt="企开得胜" class="brand-logo-img" />
          <span class="brand-logo-text">企开得胜</span>
        </div>

        <div class="brand-main">
          <h1 class="brand-title">
            产业链招商<br />
            <span class="brand-title-highlight">决策引擎</span>
          </h1>
          <p class="brand-desc">
            以数据驱动精准招商，聚合产业链、企业、专利与园区全景数据，
            为您提供智能化的招商决策支持。
          </p>
        </div>

        <div class="brand-stats">
          <div class="stat-item">
            <p class="stat-num">67</p>
            <p class="stat-label">产业链节点</p>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <p class="stat-num">3.5万+</p>
            <p class="stat-label">关联企业</p>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <p class="stat-num">5.7千+</p>
            <p class="stat-label">专利数据</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧登录表单区 -->
    <div class="form-panel">
      <div class="form-wrapper">
        <div class="form-header">
          <h2 class="form-title">欢迎回来</h2>
          <p class="form-subtitle">请使用您的账号密码登录系统</p>
        </div>

        <form @submit.prevent="handleLogin">
          <!-- Username -->
          <div class="field-group">
            <label class="field-label">用户名</label>
            <div class="field-input-wrap">
              <i class="fa-regular fa-user field-icon"></i>
              <input
                v-model="form.username"
                type="text"
                class="field-input"
                placeholder="请输入用户名"
                @focus="onFocus"
                @blur="onBlur"
              />
            </div>
          </div>

          <!-- Password -->
          <div class="field-group">
            <label class="field-label">密码</label>
            <div class="field-input-wrap">
              <i class="fa-solid fa-lock field-icon"></i>
              <input
                v-model="form.password"
                type="password"
                class="field-input"
                placeholder="请输入密码"
                @focus="onFocus"
                @blur="onBlur"
              />
            </div>
          </div>

          <!-- Region Selector -->
          <div class="field-group">
            <label class="field-label">所在地区</label>
            <div class="region-wrap" ref="regionWrapRef">
              <i class="fa-solid fa-location-dot region-icon"></i>
              <div class="region-display" :class="{ focused: regionOpen }" @click="toggleRegion">
                <span class="truncate">{{ form.region }}</span>
                <i class="fa-solid fa-chevron-down region-arrow" :class="{ rotated: regionOpen }"></i>
              </div>
              <div class="region-dropdown" :class="{ open: regionOpen }">
                <div class="region-search">
                  <div class="search-inner">
                    <i class="fa-solid fa-magnifying-glass search-icon-inner"></i>
                    <input v-model="regionQuery" type="text" placeholder="搜索城市..." @input="filterRegions" />
                  </div>
                </div>
                <div class="region-options">
                  <div v-for="r in filteredRegions" :key="r" class="region-option"
                       :class="{ active: form.region === r }" @click="selectRegion(r)">
                    <i class="fa-solid fa-check check-icon" :class="{ visible: form.region === r }"></i>
                    <span>{{ r }}</span>
                    <span v-if="r === '马鞍山市'" class="default-tag">默认</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Error Message -->
          <div v-if="errorMsg" class="error-msg">
            <i class="fa-solid fa-circle-exclamation mr-1"></i>{{ errorMsg }}
          </div>

          <!-- Submit -->
          <button type="submit" class="login-btn" :disabled="loading">
            <i class="fa-solid fa-arrow-right-to-bracket mr-2"></i>
            {{ loading ? '登录中...' : '登 录' }}
          </button>
        </form>

        <p class="form-footer">© 2026 企开得胜 - 专利驱动的产业大脑与精准招商平台</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { get } from '@/services/api'

const router = useRouter()
const authStore = useAuthStore()

const form = reactive({
  username: '',
  password: '',
  region: '马鞍山市'
})

const regionOpen = ref(false)
const regionQuery = ref('')
const errorMsg = ref('')
const loading = ref(false)

// 地区列表：预先填充常用城市，再从后端加载补充
const defaultCities = ['马鞍山市', '合肥市', '芜湖市', '南京市', '苏州市', '上海市', '北京市', '深圳市', '广州市']
const allRegions = ref([...defaultCities])
const filteredRegions = ref([...defaultCities])

// 从后端加载补充的地区列表
onMounted(async () => {
  document.addEventListener('click', onDocClick)
  try {
    const data = await get('/admin/regions?parentId=0')
    const cities = []
    for (const prov of data) {
      if (prov.children) {
        for (const city of prov.children) {
          if (prov.name === city.name) cities.push(city.name)
          else if (['北京市','天津市','上海市','重庆市'].includes(prov.name)) {
            if (!cities.includes(prov.name)) cities.push(prov.name)
          } else {
            cities.push(city.name)
          }
        }
      }
    }
    const uniqueCities = [...new Set(cities)]
    // 仅当后端返回有效城市时才覆盖默认列表（避免接口异常导致地区下拉为空）
    if (uniqueCities.length > 0) {
      allRegions.value = uniqueCities
      filteredRegions.value = uniqueCities
    }
  } catch (e) {
    console.error('Failed to load regions:', e)
  }
})

function toggleRegion() {
  regionOpen.value = !regionOpen.value
}

function selectRegion(val) {
  form.region = val
  regionOpen.value = false
}

function filterRegions() {
  const q = regionQuery.value
  filteredRegions.value = allRegions.value.filter(r => r.includes(q))
}

function onFocus(e) {
  e.target.style.borderColor = 'rgba(37,99,235,.5)'
  e.target.style.boxShadow = '0 0 0 3px rgba(37,99,235,.08)'
}

function onBlur(e) {
  e.target.style.borderColor = '#e2e8f0'
  e.target.style.boxShadow = 'none'
}

async function handleLogin() {
  errorMsg.value = ''
  loading.value = true
  try {
    await authStore.login(form.username, form.password, form.region)
    router.push('/home')
  } catch (e) {
    errorMsg.value = e.message || '登录失败，请重试'
  } finally {
    loading.value = false
  }
}

// Close region dropdown on outside click
function onDocClick(e) {
  const ref = document.querySelector('.region-wrap')
  if (ref && !ref.contains(e.target)) {
    regionOpen.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', onDocClick)
})

onUnmounted(() => {
  document.removeEventListener('click', onDocClick)
})
</script>

<style scoped>
/* ========== 页面整体：左右分栏 ========== */
.login-page {
  min-height: 100vh;
  display: flex;
  background: #f8fafc;
}

/* ========== 左侧品牌区 ========== */
.brand-panel {
  flex: 1;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(160deg, #0f1e42 0%, #1e3a8a 45%, #1e40af 75%, #1e3a8a 100%);
  padding: 3rem;
}
.brand-bg {
  position: absolute;
  inset: 0;
  overflow: hidden;
}
.brand-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255,255,255,.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,.04) 1px, transparent 1px);
  background-size: 40px 40px;
  mask-image: radial-gradient(ellipse at center, rgba(0,0,0,.6), transparent 75%);
  -webkit-mask-image: radial-gradient(ellipse at center, rgba(0,0,0,.6), transparent 75%);
}
.brand-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
}
.glow-1 {
  top: -10%;
  left: -5%;
  width: 420px;
  height: 420px;
  background: rgba(59,130,246,.35);
}
.glow-2 {
  bottom: -15%;
  right: -5%;
  width: 460px;
  height: 460px;
  background: rgba(6,182,212,.25);
}
.glow-3 {
  top: 45%;
  right: 20%;
  width: 180px;
  height: 180px;
  background: rgba(99,102,241,.2);
}

.brand-content {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 480px;
}
.brand-logo {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 3.5rem;
}
.brand-logo-img {
  width: 44px;
  height: 44px;
  object-fit: contain;
  filter: drop-shadow(0 4px 12px rgba(59,130,246,.4));
}
.brand-logo-text {
  color: #fff;
  font-size: 1.375rem;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.brand-title {
  color: #fff;
  font-size: 2.5rem;
  font-weight: 800;
  line-height: 1.25;
  letter-spacing: -1px;
  margin-bottom: 1.25rem;
}
.brand-title-highlight {
  background: linear-gradient(90deg, #60a5fa, #22d3ee);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.brand-desc {
  color: rgba(191, 219, 254, 0.6);
  font-size: 0.9375rem;
  line-height: 1.8;
  font-weight: 300;
  max-width: 400px;
}

.brand-stats {
  display: flex;
  align-items: center;
  gap: 1.75rem;
  margin-top: 3rem;
}
.stat-num {
  color: #fff;
  font-size: 1.5rem;
  font-weight: 700;
  margin-bottom: 0.25rem;
}
.stat-label {
  color: rgba(191, 219, 254, 0.5);
  font-size: 0.75rem;
}
.stat-divider {
  width: 1px;
  height: 36px;
  background: rgba(255,255,255,.15);
}

/* ========== 右侧表单区 ========== */
.form-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;
  background: #f8fafc;
}
.form-wrapper {
  width: 100%;
  max-width: 400px;
}
.form-header {
  margin-bottom: 2rem;
}
.form-title {
  font-size: 1.75rem;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 0.5rem;
}
.form-subtitle {
  color: #94a3b8;
  font-size: 0.875rem;
}

/* ========== 表单字段 ========== */
.field-group {
  margin-bottom: 1.125rem;
}
.field-label {
  color: #475569;
  font-size: 0.8125rem;
  font-weight: 600;
  display: block;
  margin-bottom: 0.5rem;
}
.field-input-wrap {
  position: relative;
}
.field-icon {
  position: absolute;
  left: 0.875rem;
  top: 50%;
  transform: translateY(-50%);
  color: #94a3b8;
  font-size: 0.875rem;
  transition: color 0.2s;
}
.field-input-wrap:focus-within .field-icon {
  color: #2563eb;
}
.field-input {
  width: 100%;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 0.75rem 0.875rem 0.75rem 2.5rem;
  color: #0f172a;
  font-size: 0.875rem;
  outline: none;
  transition: all 0.2s;
  box-shadow: 0 1px 2px rgba(0,0,0,.03);
}
.field-input::placeholder {
  color: #cbd5e1;
}
.field-input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37,99,235,.08);
}

/* ========== 地区选择 ========== */
.region-wrap {
  position: relative;
}
.region-icon {
  position: absolute;
  left: 0.875rem;
  top: 50%;
  transform: translateY(-50%);
  color: #94a3b8;
  font-size: 0.875rem;
  z-index: 2;
  transition: color 0.2s;
}
.region-wrap:focus-within .region-icon {
  color: #2563eb;
}
.region-display {
  width: 100%;
  border-radius: 10px;
  padding: 0.75rem 2.25rem 0.75rem 2.5rem;
  color: #0f172a;
  font-size: 0.875rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: space-between;
  user-select: none;
  transition: all 0.2s;
  background: #fff;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 2px rgba(0,0,0,.03);
}
.region-display.focused {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37,99,235,.08);
}
.region-arrow {
  color: #94a3b8;
  font-size: 0.625rem;
  transition: transform 0.2s;
}
.region-arrow.rotated {
  transform: rotate(180deg);
}
.region-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  margin-top: 0.375rem;
  border-radius: 10px;
  overflow: hidden;
  z-index: 20;
  background: #fff;
  border: 1px solid #e2e8f0;
  box-shadow: 0 8px 32px rgba(0,0,0,.12);
  opacity: 0;
  visibility: hidden;
  transform: translateY(-4px);
  transition: all 0.2s;
}
.region-dropdown.open {
  opacity: 1;
  visibility: visible;
  transform: translateY(0);
}
.region-search {
  padding: 0.5rem 0.75rem;
  border-bottom: 1px solid #e2e8f0;
}
.search-inner {
  position: relative;
}
.search-icon-inner {
  position: absolute;
  left: 0.625rem;
  top: 50%;
  transform: translateY(-50%);
  color: #cbd5e1;
  font-size: 0.688rem;
}
.region-search input {
  width: 100%;
  padding: 0.438rem 0.625rem 0.438rem 1.75rem;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 0.75rem;
  outline: none;
  background: #f8fafc;
  color: #334155;
  transition: all 0.2s;
}
.region-search input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 2px rgba(37,99,235,.12);
  background: #fff;
}
.region-options {
  max-height: 200px;
  overflow-y: auto;
}
.region-option {
  display: flex;
  align-items: center;
  padding: 0.625rem 0.875rem;
  cursor: pointer;
  font-size: 0.8125rem;
  color: #334155;
  transition: all 0.12s;
  border-bottom: 1px solid #f1f5f9;
}
.region-option:last-child {
  border-bottom: none;
}
.region-option:hover {
  background: #eff6ff;
  color: #1e40af;
}
.region-option.active {
  color: #1e40af;
  background: #e0efff;
  font-weight: 600;
}
.check-icon {
  font-size: 0.625rem;
  margin-right: 0.5rem;
  opacity: 0;
}
.check-icon.visible {
  opacity: 1;
  color: #2563eb;
}
.default-tag {
  margin-left: auto;
  font-size: 0.625rem;
  color: #cbd5e1;
  background: #f1f5f9;
  padding: 0.125rem 0.375rem;
  border-radius: 4px;
}

/* ========== 登录按钮 ========== */
.login-btn {
  width: 100%;
  padding: 0.75rem;
  color: #fff;
  font-size: 0.9375rem;
  font-weight: 600;
  border-radius: 10px;
  border: none;
  cursor: pointer;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  box-shadow: 0 4px 16px rgba(37,99,235,.3);
  transition: all 0.2s;
  margin-top: 0.5rem;
  letter-spacing: 0.1em;
}
.login-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  box-shadow: 0 6px 20px rgba(37,99,235,.4);
  transform: translateY(-1px);
}
.login-btn:active:not(:disabled) {
  transform: translateY(0);
}
.login-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* ========== 错误提示 ========== */
.error-msg {
  margin-bottom: 1rem;
  padding: 0.625rem 0.75rem;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  color: #dc2626;
  font-size: 0.8125rem;
}

/* ========== 页脚 ========== */
.form-footer {
  color: #cbd5e1;
  font-size: 0.75rem;
  text-align: center;
  margin-top: 2.5rem;
}

/* ========== 响应式 ========== */
@media (max-width: 900px) {
  .brand-panel {
    display: none;
  }
  .login-page {
    display: block;
  }
  .form-panel {
    min-height: 100vh;
  }
}
</style>
