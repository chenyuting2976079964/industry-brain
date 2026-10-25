# 企开得胜 - 专利驱动的产业大脑与精准招商平台 — 前端

产业链招商决策引擎，基于安徽省马鞍山市产业数据构建的数据驾驶舱。

## 技术栈

| 类别 | 技术 |
|------|------|
| 框架 | Vue 3 (Composition API) |
| 路由 | Vue Router 4 |
| 状态管理 | Pinia |
| 构建 | Vite 5 |
| CSS | Tailwind CSS 3 |
| 图标 | Font Awesome 6 (CDN) |
| 打包 | vite-plugin-singlefile → 单 HTML |

## 快速开始

```bash
# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 构建生产版本
npm run build
```

## 环境配置

复制 `.env.example` 为 `.env`，按需修改后端地址：

```env
VITE_API_BASE=http://localhost:8080
```

## 目录结构

```
产业大脑产品/
├── index.html                  # 入口 HTML
├── vite.config.js              # Vite 配置 (别名 + 单文件打包)
├── tailwind.config.js
├── public/
│   ├── favicon.png
│   ├── logo.png / logo_white.png
│   └── 马鞍山市地图.png
├── src/
│   ├── main.js                 # 应用入口 (挂载 Pinia + Router)
│   ├── App.vue                 # 根组件
│   ├── router/index.js         # 路由表 + 登录守卫
│   ├── stores/
│   │   ├── app.js              # 全局 UI 状态 (标题/预警数/大屏)
│   │   ├── auth.js             # 用户认证状态
│   │   └── eventBus.js         # 跨组件事件总线 (弹窗/导航)
│   ├── services/
│   │   ├── api.js              # API 底层封装
│   │   ├── auth.service.js     # 认证接口
│   │   └── dashboard.service.js # 首页 KPI/预警/园区接口
│   ├── mock/
│   │   └── dashboard.js        # 首页 Mock 数据
│   ├── views/
│   │   ├── LoginView.vue       # 登录页 (毛玻璃 + 地区选择)
│   │   ├── MainLayout.vue      # 主框架 (侧边栏/顶栏/弹窗/大屏)
│   │   ├── HomeDashboard.vue   # 首页看板
│   │   ├── IndustryView.vue    # 产业链全景 (何锦鹏)
│   │   ├── DiagnosisView.vue   # 强弱诊断 (何锦鹏)
│   │   ├── EnterpriseView.vue  # 企业详情 (何锦鹏)
│   │   ├── DashboardFullView.vue # 全景大屏 (何锦鹏)
│   │   ├── InvestmentView.vue  # 精准招商 (陈涛)
│   │   ├── ReportsView.vue     # 招商报告 (陈涛)
│   │   └── LocalEnterprisesView.vue # 本地企业名录
│   └── styles/
│       └── main.css            # 全局样式 (Tailwind + 组件样式)
└── dist/                       # 构建产物 (已 gitignore)
```

## 路由表

| 路径 | 页面 | 负责 |
|------|------|------|
| `/login` | 登录 | 汤一贤 |
| `/home` | 首页看板 | 汤一贤 |
| `/industry` | 产业链全景 | 何锦鹏 |
| `/diagnosis` | 强弱链诊断 | 何锦鹏 |
| `/enterprise` | 企业详情 | 何锦鹏 |
| `/investment` | 精准招商 | 陈涛 |
| `/local-enterprises` | 本地企业查看 | 共用 |
| `/reports` | 招商报告 | 陈涛 |

## 职责分工

### 汤一贤 — 前端基础载体 + 首页看板

- 登录页 (毛玻璃表单、地区选择器)
- 主框架 (侧边栏导航、顶部状态栏、全局弹窗系统、全屏大屏容器)
- 首页看板 (KPI 卡片、产业链分布、预警列表、园区统计表)
- 公共能力 (Pinia 事件总线、路由守卫、页面切换动画、sessionStorage 跨页传参)
- 对应 API: 登录/用户 3 个 + 首页 KPI/预警/园区 9 个 = **12 个**

### 何锦鹏 — 产业链核心业务

- 产业链全景 (SVG 图谱、多维度筛选)
- 强弱诊断 (链路分级、诊断计算)
- 企业详情 (三 Tab 详情、地图可视化)
- 全景大屏 (ECharts 图表自适应)
- 自研算法: **A1 专利映射**、**A2 强弱诊断**
- 对应 API: **22 个**

### 陈涛 — 招商 + 报告

- 精准招商 (筛选卡片、候选清单、分页)
- 招商报告 (模板/预览/批量 PDF+Excel 导出)
- 自研算法: **A3 匹配度**、**A4 技术画像**、**A5 招商评分**
- 对应 API: **15 个**

### 查英 — 后端基础设施

- Docker 一键部署 (MySQL8/ES8/Neo4j5/Redis7/MinIO/Canal)
- 数据层 (18 张表 DDL、22 套 Mapper、索引约束)
- Canal 实时同步 MySQL → ES / Neo4j
- 数据导入、园区管理、健康检测接口
- JWT 安全拦截、限流、文件存储
- 对应 API: **8 个**，无前端页面

## 当前状态

- 前端各页面已使用 **Mock 数据** 独立运行
- service 层已预留切换真实 API 的接口签名
- 后端准备就绪后，取消 `src/services/*.js` 中的 mock import，改为 axios 调用

## 演示账号

```
用户名: admin
密码: 123456
地区: 马鞍山市
```
