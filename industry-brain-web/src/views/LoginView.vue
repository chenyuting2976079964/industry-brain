<template>
  <div class="login-page">
    <!-- Decorative radial glows -->
    <div class="glow glow-top-left"></div>
    <div class="glow glow-bottom-right"></div>
    <div class="glow glow-center-right"></div>

    <div class="login-wrapper">
      <!-- Logo Area -->
      <div class="logo-area">
        <div class="logo-icon">
          <img src="/logo_white.png" alt="产业大脑" class="logo-img" />
        </div>
        <h1 class="logo-title">产业大脑平台</h1>
        <p class="logo-subtitle">产业链招商决策引擎</p>
      </div>

      <!-- Glass Card -->
      <div class="glass-card">
        <h2 class="card-title">欢迎登录</h2>
        <p class="card-subtitle">请使用您的账号密码登录系统</p>

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
                <div style="padding:6px 10px;border-bottom:1px solid #e2e8f0">
                  <input v-model="regionQuery" type="text" placeholder="搜索城市..."
                         @input="filterRegions"
                         style="width:100%;padding:6px 10px;border:1px solid #e2e8f0;border-radius:6px;font-size:12px;outline:none;background:#f8fafc;color:#334155;box-sizing:border-box" />
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
      </div>

      <p class="footer-text">© 2026 产业大脑平台 · 数据驱动精准招商</p>
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
    allRegions.value = uniqueCities
    filteredRegions.value = uniqueCities
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
  e.target.style.borderColor = 'rgba(59,130,246,.5)'
  e.target.style.boxShadow = '0 0 0 3px rgba(59,130,246,.1)'
}

function onBlur(e) {
  e.target.style.borderColor = 'rgba(255,255,255,.15)'
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
/* ========== Page Background ========== */
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  padding: 1rem;
  background: linear-gradient(135deg, #0f1e42 0%, #1e3a8a 40%, #1e40af 70%, #0f1e42 100%);
}
.login-page::before {
  content: '';
  position: fixed;
  inset: 0;
  background-image: radial-gradient(rgba(255,255,255,.04) 1px, transparent 1px);
  background-size: 32px 32px;
  pointer-events: none;
}

/* ========== Glows ========== */
.glow {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
}
.glow-top-left {
  top: -10%;
  left: -5%;
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(59,130,246,.15), transparent 70%);
}
.glow-bottom-right {
  bottom: -15%;
  right: -5%;
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, rgba(6,182,212,.1), transparent 70%);
}
.glow-center-right {
  top: 40%;
  right: 15%;
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, rgba(99,102,241,.08), transparent 70%);
}

/* ========== Wrapper ========== */
.login-wrapper {
  position: relative;
  z-index: 10;
  width: 100%;
  max-width: 420px;
}

/* ========== Logo ========== */
.logo-area {
  text-align: center;
  margin-bottom: 2rem;
}
.logo-icon {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  margin: 0 auto 1rem;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #3b82f6, #06b6d4);
  box-shadow: 0 8px 24px rgba(59,130,246,.25);
}
.logo-img {
  width: 40px;
  height: 40px;
  object-fit: contain;
}
.logo-title {
  font-size: 1.875rem;
  font-weight: 700;
  color: #fff;
  letter-spacing: -0.5px;
}
.logo-subtitle {
  color: rgba(191, 219, 254, 0.5);
  margin-top: 0.5rem;
  font-size: 0.875rem;
  font-weight: 300;
}

/* ========== Glass Card ========== */
.glass-card {
  background: rgba(255,255,255,.06);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255,255,255,.1);
  border-radius: 16px;
  padding: 2rem;
}
.card-title {
  color: #fff;
  font-size: 1.125rem;
  font-weight: 600;
  margin-bottom: 0.25rem;
}
.card-subtitle {
  color: rgba(191, 219, 254, 0.5);
  font-size: 0.875rem;
  margin-bottom: 1.5rem;
}

/* ========== Field ========== */
.field-group {
  margin-bottom: 1rem;
}
.field-label {
  color: rgba(191, 219, 254, 0.7);
  font-size: 0.75rem;
  font-weight: 500;
  display: block;
  margin-bottom: 0.375rem;
}
.field-input-wrap {
  position: relative;
}
.field-icon {
  position: absolute;
  left: 0.875rem;
  top: 50%;
  transform: translateY(-50%);
  color: rgba(147, 197, 253, 0.4);
  font-size: 0.875rem;
}
.field-input {
  width: 100%;
  background: rgba(255,255,255,.08);
  border: 1px solid rgba(255,255,255,.15);
  border-radius: 12px;
  padding: 0.625rem 0.875rem 0.625rem 2.5rem;
  color: #fff;
  font-size: 0.875rem;
  outline: none;
  transition: all 0.2s;
}
.field-input::placeholder {
  color: rgba(147, 197, 253, 0.3);
}

/* ========== Region Selector ========== */
.region-wrap {
  position: relative;
}
.region-icon {
  position: absolute;
  left: 0.875rem;
  top: 50%;
  transform: translateY(-50%);
  color: rgba(147, 197, 253, 0.4);
  font-size: 0.875rem;
  z-index: 2;
}
.region-display {
  width: 100%;
  border-radius: 12px;
  padding: 0.625rem 2.25rem 0.625rem 2.5rem;
  color: #fff;
  font-size: 0.875rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: space-between;
  user-select: none;
  transition: all 0.2s;
  background: rgba(255,255,255,.08);
  border: 1px solid rgba(255,255,255,.15);
}
.region-display.focused {
  border-color: rgba(59,130,246,.5);
  box-shadow: 0 0 0 3px rgba(59,130,246,.1);
}
.region-arrow {
  color: rgba(147, 197, 253, 0.4);
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
  border-radius: 12px;
  overflow: hidden;
  z-index: 20;
  background: #fff;
  border: 1px solid #e2e8f0;
  box-shadow: 0 8px 32px rgba(0,0,0,.1);
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
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59,130,246,.12);
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

/* ========== Login Button ========== */
.login-btn {
  width: 100%;
  padding: 0.625rem;
  color: #fff;
  font-size: 0.875rem;
  font-weight: 600;
  border-radius: 12px;
  border: none;
  cursor: pointer;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  box-shadow: 0 4px 16px rgba(37,99,235,.3);
  transition: all 0.2s;
}
.login-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  box-shadow: 0 6px 20px rgba(37,99,235,.4);
}
.login-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* ========== Error ========== */
.error-msg {
  margin-bottom: 1rem;
  padding: 0.5rem 0.75rem;
  background: rgba(239, 68, 68, 0.15);
  border: 1px solid rgba(239, 68, 68, 0.3);
  border-radius: 8px;
  color: #fca5a5;
  font-size: 0.75rem;
}

/* ========== Footer ========== */
.footer-text {
  color: rgba(191, 219, 254, 0.2);
  font-size: 0.75rem;
  text-align: center;
  margin-top: 1.5rem;
}
</style>
