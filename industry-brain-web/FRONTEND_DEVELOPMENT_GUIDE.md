# 产业大脑平台前端开发指南

## 1. 项目概览

**项目名称**: industry-brain-platform（产业大脑平台 v2.0）  
**技术栈**: Vue 3 + Vite + Pinia + Vue Router + ECharts + Tailwind CSS  
**架构风格**: 单页应用（SPA），前后端分离，Mock Server 调试模式  

> 本前端是为"马鞍山市产业链精准招商决策支持系统"配套开发的可视化数据中台，对接后端 Spring Boot 微服务（/api/v1 接口层）。

---

## 2. 目录结构

```
industry-brain-web/
├── src/                  # 源代码
│   ├── components/       # （预留，当前未使用）
│   ├── views/            # 页面视图组件 (10 个)
│   │   ├── DashboardFullView.vue  # 大屏全景视图
│   │   ├── DiagnosisView.vue      # 强弱诊断视图
│   │   ├── EnterpriseView.vue     # 企业详情视图
│   │   ├── HomeDashboard.vue      # 首页看板视图
│   │   ├── IndustryView.vue       # 产业链全景视图
│   │   ├── InvestmentView.vue     # 精准招商视图
│   │   ├── LocalEnterprisesView.vue # 本地企业查询视图
│   │   ├── LoginView.vue          # 登录页
│   │   ├── MainLayout.vue         # 主布局（侧边栏+顶部栏）
│   │   └── ReportsView.vue        # 招商报告视图
│   ├── stores/             # Pinia 状态管理
│   │   ├── app.js           # 全局应用状态（页面标题、全屏控制等）
│   │   ├── auth.js          # 用户认证状态（token、角色、权限）
│   │   └── eventBus.js      # 跨页面通信总线（替代 postMessage）
│   ├── services/           # API 业务服务层（共 9 个模块）
│   │   ├── api.js           # 统一 fetch 请求封装（A00）
│   │   ├── auth.service.js  # 鉴权服务（A01-A03）
│   │   ├── dashboardfullview.service.js # 大屏数据服务（F01-F06）
│   │   ├── diagnosis.service.js   # 强弱诊断服务（D01-D04）
│   │   ├── enterprise.service.js  # 企业服务（E01-E05）
│   │   ├── homedashboard.service.js # 首页看板服务（H01-H11）
│   │   ├── industryChain.service.js # 产业链服务（C01-C09）
│   │   ├── investment.service.js  # 招商匹配服务（I01-I07）
│   │   └── reports.service.js     # 招商报告服务（R01-R08）
│   ├── composables/        # 组合式函数
│   │   ├── useEcharts.js    # ECharts 图表钩子
│   │   └── useModal.js      # 弹窗交互封装
│   ├── router/             # 路由配置
│   │   └── index.js         # 11 条路由 + 守卫
│   ├── styles/             # 全局样式
│   │   └── main.css         # Tailwind + 基础重置
│   ├── App.vue              # 根组件（仅 router-view）
│   └── main.js              # 应用入口（创建实例、注入 echarts、启动）
├── dist/                    # Vite 构建输出
├── node_modules/            # 依赖包
├── .env.example             # 环境变量模板
├── .gitignore               # Git 忽略规则
├── index.html               # 入口 HTML
├── package.json             # 脚本与依赖
├── tailwind.config.js       # Tailwind 配置
├── vite.config.js           # Vite 配置（含代理）
└── README.md                # 项目说明
```

---

## 3. 核心技术说明

### 3.1 构建工具：Vite

- **dev**: `vite` — 启动开发服务器（端口 3001，自动打开浏览器）
- **build**: `vite build` — 生产环境打包生成静态文件
- **preview**: `vite preview` — 预览构建结果

**Vite 特性**:
- 使用 `@vitejs/plugin-vue` 支持 `.vue` 单文件组件
- 使用 `vite-plugin-singlefile` 将多个 Vue SFC 打包为单个 HTML 文件（适合部署场景）
- ES Module alias: `@` → `/src`
- 开发时代理 `/api/v1` → 后端 Mock Server (http://localhost:8080)

```js
// vite.config.js
server: {
  port: 3001,
  proxy: {
    '/api/v1': {
      target: 'http://localhost:8080',
      changeOrigin: true,
      rewrite: (path) => path.replace(/^\/api\/v1/, ''),
      ws: true
    }
  }
}
```

### 3.2 状态管理：Pinia

两个核心 Store：

| Store | 职责 | 关键状态 |
|-------|------|----------|
| `useAuthStore` | 用户认证与权限 | `user`, `token`, `region`, `roles`, `isLoggedIn`, `hasRole()`, `hasPermission()` |
| `useAppStore` | 应用全局状态 | `pageTitle`, `currentPage`, `alertCount`, `lastUpdateTime`, `showDashOverlay`（全屏控制） |

**认证流程**:
1. 登录：`authStore.login(username, password, region)` → 存储 token 到 sessionStorage
2. 守卫检测：路由 `beforeEach` 检查 sessionStorage 中有无 token
3. 退出：清除所有 sessionStorage 和 Store 状态，跳转至登录页

### 3.3 路由：Vue Router

采用 hash 模式（`createWebHashHistory`），便于部署在无后端支持的静态服务器上。

#### 路由表

| 路径 | 名称 | 组件 | 元数据 |
|------|------|------|--------|
| `/login` | Login | `LoginView.vue` | 标题：登录 |
| `/` (重定向 → `/home`) | - | `MainLayout.vue` | — |
| `/home` | Home | `HomeDashboard.vue` | 标题：首页看板 |
| `/local-enterprises` | LocalEnterprises | `LocalEnterprisesView.vue` | 标题：查看本地企业 |
| `/industry` | Industry | `IndustryView.vue` | 标题：产业链全景 |
| `/diagnosis` | Diagnosis | `DiagnosisView.vue` | 标题：强弱诊断 |
| `/investment` | Investment | `InvestmentView.vue` | 标题：精准招商 |
| `/enterprise` | Enterprise | `EnterpriseView.vue` | 标题：企业详情 |
| `/reports` | Reports | `ReportsView.vue` | 标题：招商报告 |

**全局守卫逻辑**:
- `/login` 页可无授权访问
- 其他路由若未登录则重定向至 `/login`
- 登录后记录 `sessionStorage.loggedIn = '1'`

### 3.4 通信机制：EventBus

用 Pinia Store 实现的事件总线（`eventBus.store`），替代了传统的 `window.postMessage`，用于：

- 全局弹窗打开/关闭（`openModal` / `closeModal`）
- 主布局与大屏视图之间的全屏切换（`showDash` / `exitDash`）
- 页面间上下文传递（`navigate`，通过 `sessionStorage.navContext` 中转）

### 3.5 API 请求层

统一的 `services/api.js` 封装：

- 基础 URL: `/api/v1`
- 自动携带 Authorization Bearer token（从 sessionStorage 读取）
- 错误处理：401 自动清理 token 并抛出异常；其他 JSON 响应码非 200 抛出错误消息

各业务服务模块在此基础上按 Controller 命名空间进行划分（如 `/auth`、`/diagnosis`、`/enterprise` 等）。

### 3.6 组合式函数

- **`useEcharts(containerRef)`**: 为 Vue 3 组件提供 ECharts 初始化、设置 option、响应式大小调整的封装。使用时传入 ref 指向 DOM 容器。
- **`useModal()`**: 弹窗交互封装器，支持通过 `data-action` 属性绑定按钮点击回调，简化弹窗内交互逻辑。

---

## 4. 页面功能详解

### 4.1 登录页 (`LoginView.vue`)

- **功能**: 用户名/密码输入、地区选择（下拉搜索）、登录提交
- **特效**: 玻璃拟态卡片、装饰性光斑背景、带区域搜索的下拉选择器
- **演示账号**: `admin / 123456`（默认预填）
- **错误处理**: 捕获网络错误（Mock Server 未启动时显示友好提示）

### 4.2 主布局 (`MainLayout.vue`)

- **左侧导航栏**: 固定收起式侧边栏，含 Logo、7 个导航项（首页看板、查看本地企业、产业链全景、强弱诊断、精准招商、招商报告）和用户信息区
- **顶部栏**: 页面标题、实时时间（秒级更新）、预警计数、系统状态指示
- **内容区**: `<router-view>` 动态渲染子页面，带有淡入过渡动画
- **全屏大屏视图**: 通过 `Teleport` 挂载至 body，独立于主布局浮动，Esc 键退出
- **全局模态框**: 通过事件总线控制的浮层弹窗

### 4.3 首页看板 (`HomeDashboard.vue`)

- **KPI 三卡**: 专利总量、企业数量、强弱链诊断概览（点击进入对应详情页模态框）
- **产业链分布进度条**: 上/中/下游企业占比可视化
- **待处理预警列表**: 红/黄/蓝三级预警，支持查看详情和处理标记
- **园区载体统计表**: 列出辖区各园区的企业数、专利数、主导产业
- **交互**: 所有 KPI 卡片点击触发模态框展示更详细分析（如专利构成、企业梯队分类）

### 4.4 产业链全景 (`IndustryView.vue`)

- **核心视图**: 三列垂直排布的节点图谱（上游→中游→下游），节点大小根据专利数量动态计算
- **连线绘制**: SVG 曲线连接上下游节点，体现技术供应关系
- **筛选功能**: 按层级（上/中/下游）、按状态（强/补/弱/缺）、关键词搜索
- **节点交互**: 点击节点显示右侧抽屉面板，展示企业列表、高价值专利、招商建议
- **链切换**: 顶部 Tab 切换不同产业链（如智能制造、集成电路等）

### 4.5 强弱诊断 (`DiagnosisView.vue`)

- **诊断卡片**: 强链/补链/弱链/缺链四个统计卡片（点击过滤表格）
- **诊断表格**: 产业链节点清单，含所属层级、专利数量、高价值专利、诊断结果、专利密集度
- **筛选**: 全部/强链/补链/弱链/缺链快速筛选
- **重新诊断**: 发起后端 recalcul 任务，轮询任务状态，完成后刷新数据
- **节点穿透**: 点击表格行或"穿透"按钮打开右侧抽屉，查看该企业领域主要专利持有人列表及招商推荐

### 4.6 精准招商 (`InvestmentView.vue`)

- **目标企业推荐列表**: 基于薄弱环节自动匹配潜在招商对象
- **多维度筛选**: 薄弱环节类型、匹配度阈值、企业规模、排序方式（匹配度/专利实力/产业带动）
- **企业卡片**: 显示匹配度环形评分、技术匹配度/专利实力/产业带动潜力三项进度条
- **操作**: 加入收藏夹清单、查看详情、导出招商清单、重新匹配算法
- **上下文感知**: 从诊断页面传递的 `navContext.fromNode` 自动填充筛选条件

### 4.7 企业详情 (`EnterpriseView.vue`)

- **三级导航**: 技术画像 / 专利列表 / 综合评估三个标签页
- **技术画像**: 技术优势领域条形图、研发方向标签云、技术短板提示
- **专利列表**: 分页表格，支持按专利类型筛选、关键词搜索
- **综合评估**: 招商评级（A/B/C）、三维度评分图、落地建议和政策匹配度
- **交互**: 点击优势项弹出详细说明模态框，专利条目点击查看专利详情

### 4.8 本地企业查询 (`LocalEnterprisesView.vue`)

- **企业列表**: 内置模拟数据（20家示例企业），按地区/园区/类型/关键词四维过滤
- **视图切换**: 卡片网格视图 / 列表视图
- **详情穿透**: 点击任意企业卡片跳转到企业详情页

### 4.9 招商报告 (`ReportsView.vue`)

- **已生成报告列表**: 名称、日期、描述、预览/下载/删除操作
- **生成新报告**: 选择报告类型、产业链范围、目标企业数量、输出格式（Excel/PDF）
- **报告模板**: 预设模板库，快速生成标准化报告
- **统计信息**: 本月生成数、累计导出数、覆盖企业数

### 4.10 大屏全景 (`DashboardFullView.vue`)

- **KPI 条**: 专利总量、覆盖企业数、弱链环节数（三栏横向排列）
- **产业链强弱分布**: 水平进度条展示四类节点的数量占比
- **企业分布**: 五宫格区域分布展示
- **高价值专利 TOP5**: 列表展示头部专利及其申请人
- **实时时钟**: 秒级更新的右上角时间显示
- **地图背景**: 暗色版区域地图作为装饰性背景（固定图片）

---

## 5. API 接口文档汇总

### 5.1 鉴权服务 (auth)

| 方法 | 端点 | 描述 |
|------|------|------|
| POST | `/auth/login` | A01 用户登录 |
| POST | `/auth/logout` | A02 用户注销 |
| GET | `/auth/info` | A03 获取当前用户信息 |

### 5.2 产业链服务 (industry/chain)

| 方法 | 端点 | 描述 |
|------|------|------|
| GET | `/industry/chain/list` | C01 获取产业链列表 |
| GET | `/industry/chain/{id}/nodes` | C02 获取产业链所有节点 |
| GET | `/industry/chain/{id}/links` | C03 获取节点间关联关系 |
| GET | `/industry/chain/node/{id}` | C04 获取节点详情 |
| POST | `/industry/chain/node` | C05 创建节点 |
| PUT | `/industry/chain/node/{id}` | C06 更新节点 |
| DELETE | `/industry/chain/node/{id}` | C07 删除节点 |
| POST | `/industry/chain/link` | C08 创建关联 |
| DELETE | `/industry/chain/link/{id}` | C09 删除关联 |

### 5.3 诊断服务 (diagnosis)

| 方法 | 端点 | 描述 |
|------|------|------|
| GET | `/diagnosis/list?status=&regionCode=` | D01 获取诊断列表 |
| GET | `/diagnosis/node/{id}` | D02 获取节点诊断详情 |
| POST | `/diagnosis/recalc?regionCode=` | D03 触发重新诊断 |
| GET | `/diagnosis/recalc/status/{taskId}` | D03 查询诊断任务状态 |
| GET | `/diography/summary?regionCode=` | D04 获取诊断汇总 |

### 5.4 企业服务 (enterprise)

| 方法 | 端点 | 描述 |
|------|------|------|
| GET | `/enterprise/{id}` | E01 获取企业基本信息 |
| GET | `/enterprise/{id}/profile` | E02 获取企业技术画像 |
| GET | `/enterprise/{id}/patents` | E03 获取企业专利列表 |
| GET | `/enterprise/{id}/evaluate` | E04 获取企业综合评估 |
| GET | `/enterprise/search?keyword=` | E05 企业模糊搜索 |
| GET | `/enterprise/local/list` | 本地企业列表 |

### 5.5 首页看板服务 (homedashboard)

| 方法 | 端点 | 描述 |
|------|------|------|
| GET | `/homedashboard/kpi` | H01 首页 KPI |
| GET | `/homedashboard/chain-dist` | H02 产业链分布 |
| GET | `/homedashboard/alerts` | H03 待处理预警 |
| PUT | `/homedashboard/alerts/{id}/dismiss` | H04 标记预警已处理 |
| GET | `/homedashboard/park-stats` | H05 园区统计 |
| GET | `/homedashboard/patent-detail` | H06 专利详情 |
| GET | `/homedashboard/enterprise-detail` | H07 企业详情 |
| GET | `/homedashboard/diagnosis-summary` | H08 诊断汇总 |
| GET | `/homedashboard/alert-detail/{id}` | H09 预警详情 |
| GET | `/homedashboard/park-patent-detail?parkName=` | H10 园区专利详情 |
| GET | `/homedashboard/park-highvalue-patents?parkName=` | H11 园区高价值专利 |

### 5.6 大屏服务 (dashboardfullview)

| 方法 | 端点 | 描述 |
|------|------|------|
| GET | `/dashboardfullview/kpi?regionCode=` | F01 KPI 数据 |
| GET | `/dashboardfullview/enterprise-dist?regionCode=` | F02 企业分布 |
| GET | `/dashboardfullview/tech-dist?regionCode=` | F03 技术领域分布 |
| GET | `/dashboardfullview/patent-top5?regionCode=` | F04 高价值专利TOP5 |
| GET | `/dashboardfullview/chain-strength?regionCode=` | F05 产业链强弱分布 |
| GET | `/dashboardfullview/map-markers?regionCode=` | F06 地图标注点 |

### 5.7 招商推荐服务 (recommendation)

| 方法 | 端点 | 描述 |
|------|------|------|
| GET | `/recommendation/match-list?weakness=&matchMin=&scale=&sortBy=&keyword=` | I01 获取匹配企业列表 |
| GET | `/recommendation/{id}?nodeId=` | I02 获取企业详情 |
| GET | `/recommendation/wishlist` | I03 获取收藏夹 |
| POST | `/recommendation/wishlist` | I04 添加至收藏 |
| DELETE | `/recommendation/wishlist/{id}` | I05 移除收藏 |
| POST | `/recommendation/rematch?nodeId=` | I06 重新匹配 |
| POST | `/recommendation/export` | I07 导出清单 |

| 方法 | 端点 | 描述 |
|------|------|------|
| GET | `/recommendation/reports` | R01 获取报告列表 |
| GET | `/recommendation/reports/{id}` | R02 获取报告详情 |
| POST | `/recommendation/reports/generate` | R03 生成新报告 |
| GET | `/recommendation/reports/{id}/download` | R04 下载报告 |
| DELETE | `/recommendation/reports/{id}` | R05 删除报告 |
| GET | `/recommendation/reports/templates` | R06 获取模板列表 |
| GET | `/recommendation/reports/stats` | R07 获取统计 |
| POST | `/recommendation/reports/export-all` | R08 批量导出 |

---

## 6. 开发与运行指南

### 6.1 前置条件

1. **安装 Node.js** (建议 LTS 版本 18.x+)
2. **启动后端 Mock Server**（前端依赖后端接口）

```bash
# 方式一：使用简单 Mock 服务器
node simple-mock-server.js

# 方式二：完整 Mock 服务器（需配合项目根目录 mock-server.js）
node mock-server.js
```

默认端口：8080（Vite 代理配置指向此端口）

### 6.2 启动开发服务器

```cd industry-brain-web
npm install          # 首次安装依赖
npm dev              # 启动 Vite 开发服务器 (http://localhost:3001)
```

浏览器自动打开登录页，使用演示账号 `admin/123456` 登录。

### 6.3 构建生产版本

```bash
npm run build
```

输出到 `dist/` 目录，可直接部署至任何静态文件服务器（Nginx、Apache、GitHub Pages 等）。

### 6.4 预览构建结果

```bash
npm run preview
```

启动本地预览服务器，查看生产构建效果。

---

## 7. 代码规范约定

- **文件命名**: 所有文件和文件夹使用 camelCase（如 `useEcharts.js`）或 kebab-case（CSS 类名）
- **注释**: 公共 API 文件头部标注接口编号和功能说明（如 `/** A01 — POST /api/v1/auth/login */`）
- **组件结构**: `<template><script setup><style scoped>` 标准 Vue 3 SFC 格式
- **状态管理**: 所有共享状态必须通过 Pinia Store 管理，禁止跨组件 prop 链传递深层数据
- **路由守卫**: 登录检查统一放在 `router/index.js` 的 `beforeEach` 中
- **错误处理**: 所有 async API 调用均包裹在 try/catch 中，用户友好的错误提示通过 eventBus 模态框展示

---

## 8. 常见问题排查

| 问题 | 可能原因 | 解决方案 |
|------|----------|----------|
| 页面空白，控制台报 CORS 错误 | Mock Server 未启动或端口不对 | 确保 `simple-mock-server.js` 运行在 8080 端口 |
| 登录后进不去主页 | sessionStorage token 丢失或被清空 | 检查浏览器是否阻止第三方 Cookie，尝试无痕模式 |
| 图表不显示 | ECharts 未正确注入 | 确认 `main.js` 中 `app.provide('echarts', echarts)` 已执行 |
| 路由导航失败 | Vue Router 模式配置冲突 | 当前为 hash 模式，确保服务器不拦截 `.html` 请求 |
| 构建后页面资源路径错乱 | Vite base 配置问题 | `vite.config.js` 中 `base: './'` 已适配相对路径部署 |

---

## 9. 扩展建议

1. **引入 VUEX 替代方案评估**: 随着状态复杂度上升，可考虑 Pinia 的分店或分模块拆分
2. **API 请求缓存**: 对读多写少的数据（如产业链目录、园区列表）增加localStorage缓存策略
3. **权限菜单动态加载**: 根据 `user.roles` 动态生成侧边栏菜单项
4. **TypeScript 迁移**: 当前为纯 JS，建议逐步迁移到 TS 提升类型安全
5. **单元测试**: 对 service 层的 API 封装和 store 的业务逻辑添加 Jest/Vitest 测试

---

*最后更新时间：2026-07-28 | 版本号：v2.0.0*