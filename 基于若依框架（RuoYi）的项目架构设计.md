# 基于若依框架（RuoYi）的项目架构设计



## 一、架构概述

本项目基于若依（RuoYi）框架构建，严格遵循若依标准的 **Controller → Service → Mapper** 三层架构。在保留若依原有权限管理、代码生成等基础能力之上，扩展**产业链智能分析**与**招商推荐**等业务模块。所有算法能力（专利映射、强弱诊断、匹配度计算、招商评分等）均通过 **Service 层接口与实现类**承载——定义接口契约，实现类封装具体算法逻辑，通过增加接口数量和实现类来扩展功能，做到"改算法不改架构"。



---







## 二、分层架构（按若依规范）

```
┌─────────────────────────────────────────────────────────────┐
│                     前端（Vue3 + Element Plus）              │
│   ── 产业全景大屏 ── 产业链图谱 ── 诊断面板 ── 招商推荐     │
└─────────────────────────────────────────────────────────────┘
                              │ HTTPS + JWT
┌─────────────────────────────────────────────────────────────┐
│                    Controller 层（若依标准）                 │
│  • 接收请求、参数校验（@Validated）                          │
│  • 权限注解（@RequiresPermissions / @PreAuthorize）         │
│  • 响应封装（AjaxResult 统一返回）                           │
│  ─── 对应若依的 `controller` 包                            │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                  Service 层（若依标准）                      │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  接口（Interface）—— 定义业务契约                     │   │
│  │  • IIndustryChainService / IRecommendationService 等 │   │
│  │  • 定义方法签名，声明事务边界，JavaDoc 约定行为       │   │
│  └──────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  实现类（ServiceImpl）—— 实现全部业务逻辑             │   │
│  │  • 流程编排： 先查什么、后算什么、最终组装什么        │   │
│  │  • 算法执行： 专利→产业链映射、强弱诊断、匹配度计算、  │   │
│  │    招商价值评分等核心算法均在此落地                    │   │
│  │  • 数据路由： 简单查询→MySQL Mapper                   │   │
│  │              全文搜索→ES Mapper                       │   │
│  │              关系分析→Neo4j Mapper                     │   │
│  │  • 通过增加接口和实现类扩展功能，不修改已有代码       │   │
│  │  ─── 对应若依的 `service` + `service.impl` 包        │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                  Mapper 层（若依 MyBatis）                   │
│  • 常规 CRUD（继承 BaseMapperPlus）                          │
│  • 复杂查询自定义 SQL（XML 映射文件）                        │
│  • 多数据源支持（MySQL / ES / Neo4j 分别注入）              │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                  基础设施（Infrastructure）                   │
│  • 缓存（Redis）• 搜索引擎（ES）• 图数据库（Neo4j）         │
│  • 对象存储（MinIO）• 安全拦截器 • 全局异常处理             │
└─────────────────────────────────────────────────────────────┘
```

> **说明**：本项目严格遵循若依框架原生的 Controller\-Service\-Mapper 三层架构。Service 层通过 **接口（Interface）\+ 实现类（ServiceImpl）** 的组合承载所有业务逻辑和算法能力——接口定义契约，实现类封装具体逻辑（包括专利映射、强弱诊断、匹配度计算等复杂算法）。通过增加接口和实现类来扩展功能，不改动已有代码，保持开闭原则。
> 
> 



---







## 三、模块目录结构（基于若依规范）

```
com.industrybrain
├── controller                 # 若依风格 Controller
│   ├── auth                   # 登录、登出、当前用户（使用若依SecurityUtils）
│   ├── industry               # 产业链全景、大屏、节点详情
│   ├── enterprise             # 企业信息、能力评估
│   ├── recommendation         # 招商推荐、技术画像、导出
│   └── admin                  # 数据导入管理
├── service                    # 业务服务层（接口）
│   ├── IndustryChainService      # 产业链业务接口
│   ├── RecommendationService     # 招商推荐业务接口
│   ├── EnterpriseService         # 企业信息业务接口
│   ├── DataImportService         # 数据导入业务接口
│   ├── ExportService             # 报告导出业务接口
│   ├── PatentMappingService      # 专利→产业链映射算法接口
│   ├── DiagnosisService          # 强弱链诊断算法接口
│   ├── MatchScoreService         # 技术匹配度算法接口
│   └── TechnicalProfileService   # 技术画像算法接口
├── service.impl               # 业务服务层（实现类）
│   ├── IndustryChainServiceImpl    # 产业链业务实现（编排+算法调用）
│   ├── RecommendationServiceImpl  # 招商推荐业务实现
│   ├── EnterpriseServiceImpl       # 企业信息业务实现
│   ├── DataImportServiceImpl       # 数据导入业务实现
│   ├── ExportServiceImpl           # 报告导出业务实现
│   ├── PatentMappingServiceImpl    # 专利映射算法实现（规则引擎/ML切换）
│   ├── DiagnosisServiceImpl        # 强弱诊断算法实现
│   ├── MatchScoreServiceImpl       # 匹配度算法实现
│   └── TechnicalProfileServiceImpl # 技术画像算法实现
├── mapper                     # MyBatis Mapper 接口
│   │                          # 继承若依 BaseMapperPlus<T>
│   ├── mysql                  # MySQL 数据访问
│   │   ├── EnterpriseMapper
│   │   ├── PatentMapper
│   │   ├── DiagnosisResultMapper
│   │   └── AuditLogMapper
│   ├── elasticsearch          # ES 数据访问
│   │   ├── PatentSearchMapper
│   │   └── EnterpriseSearchMapper
│   └── neo4j                  # Neo4j 数据访问
│       ├── IndustryChainGraphMapper
│       └── EnterpriseRelationMapper
├── domain                     # 领域实体（POJO / BO）
│   ├── Enterprise.java        # 企业实体
│   ├── Patent.java            # 专利实体
│   ├── IndustryChainNode.java # 产业链节点实体
│   ├── DiagnosisResult.java   # 诊断结果业务对象
│   └── MatchScoreResult.java  # 匹配度结果业务对象
├── config                     # 配置类（若依扩展）
│   ├── MultiDataSourceConfig
│   ├── Neo4jConfig
│   ├── ElasticsearchConfig
│   └── RedisConfig
├── interceptor                # 拦截器（区域数据隔离、限流）
│   └── RegionDataIsolationInterceptor
├── security                   # 安全扩展（继承若依SecurityConfig）
│   ├── JwtTokenProvider
│   └── RateLimitInterceptor
├── utils                      # 工具类
└── dto                        # 数据传输对象（request/response）
```

> **注意**：若依默认包名通常为 `com.ruoyi`，此处可根据实际项目修改为 `com.industrybrain`。Service 层分为 `service`（接口包）和 `service.impl`（实现类包），严格遵循若依原生包结构——接口定义业务契约，实现类承载所有业务逻辑（包括流程编排和算法执行），通过新增接口\+实现类扩展功能。

---





## 四、核心业务模块划分（对应产品功能）

|产品模块|主要 Controller|Service 接口|Service 实现类|调用的 Mapper|
|---|---|---|---|---|
|**看产业** – 产业链全景、大屏|IndustryController|IIndustryChainService \+ IPatentMappingService|IndustryChainServiceImpl \+ PatentMappingServiceImpl|EnterpriseMapper、PatentMapper、PatentSearchMapper、IndustryChainGraphMapper|
|**判强弱** – 强弱诊断、企业穿透|IndustryController \+ EnterpriseController|IIndustryChainService \+ IEnterpriseService \+ IDiagnosisService|IndustryChainServiceImpl \+ EnterpriseServiceImpl \+ DiagnosisServiceImpl|PatentMapper、PatentSearchMapper、DiagnosisResultMapper、EnterpriseRelationMapper|
|**选目标** – 弱链定位、目标筛选|RecommendationController|IRecommendationService \+ IMatchScoreService|RecommendationServiceImpl \+ MatchScoreServiceImpl|EnterpriseSearchMapper、PatentSearchMapper、IndustryChainGraphMapper、MatchScoreResult 实体|
|**引进来** – 技术画像、招商评估|RecommendationController|IRecommendationService \+ IExportService \+ ITechnicalProfileService|RecommendationServiceImpl \+ ExportServiceImpl \+ TechnicalProfileServiceImpl|EnterpriseMapper、PatentMapper、EnterpriseRelationMapper|
|**全局** – 用户登录、权限、日志|AuthController（若依自带）|SysLoginService（若依自带）|SysLoginServiceImpl（若依自带）|SysUserMapper（若依自带）、AuditLogMapper|



---



## 五、技术选型（基于若依 \+ 扩展）



|层次|技术选型|说明|
|---|---|---|
|**基础框架**|RuoYi\-Vue \(Spring Boot 2\.x / 3\.x\)|含权限管理、代码生成、定时任务等|
|**关系数据库**|MySQL 8\.0|业务主数据|
|**ORM**|MyBatis\-Plus（若依默认使用MyBatis）|或保持若依原生MyBatis|
|**搜索引擎**|Elasticsearch 8\.x|全文检索、专利/企业搜索|
|**图数据库**|Neo4j 5\.x Enterprise|产业链结构、关系查询、向量索引|
|**缓存与会话**|Redis 7\.x|缓存、JWT黑名单、限流计数|
|**对象存储**|MinIO|导出文件、报告存储|
|**权限控制**|Apache Shiro（若依默认） / Spring Security|本项目推荐Spring Security（若依Vue版已集成）|
|**前端框架**|Vue 3 \+ Element Plus \+ ECharts \+ D3\.js|若依自带Vue前端，扩展图表库|



---



## 六、数据存储方案（多源融合）



|存储组件|存储内容|访问方式|
|---|---|---|
|**MySQL**|用户、企业工商信息、专利全量字段、诊断结果、推荐记录、审计日志、映射规则|MyBatis Mapper \+ 事务|
|**Elasticsearch**|专利/企业索引（用于全文搜索、聚合统计）|RestHighLevelClient / Spring Data Elasticsearch|
|**Neo4j**|产业链节点关系、企业\-专利关系、引用网络、协同投资关系|Spring Data Neo4j / Driver|
|**Redis**|会话、JWT黑名单、限流计数器、业务缓存（大屏、产业链全景）|RedisTemplate|
|**MinIO**|导出Excel/PDF、技术画像报告|MinIO SDK|



**数据同步策略**：  

- MySQL 作为“唯一真相来源”，ES 和 Neo4j 通过 **Canal 监听 binlog** 准实时同步（延迟 \<1s），或由数据导入任务统一写入三端。

    

---



## 七、安全与权限（若依扩展）



若依已提供完善的 RBAC 权限管理（用户、角色、菜单、按钮）。本项目在此基础上增加：



|安全机制|实现方式|对应风险|
|---|---|---|
|**区域数据隔离**|自定义 `RegionDataIsolationInterceptor`，在 SQL/ES/Neo4j 查询中自动追加 `region_code` 过滤|越权访问|
|**JWT黑名单**|扩展若依 JWT 工具，登出后将 token 存入 Redis 黑名单|会话管理漏洞|
|**接口限流**|自定义 `RateLimitInterceptor`，基于 IP\+用户 的令牌桶限流|并发压力/暴力破解|
|**密码策略**|继承若依密码策略，增加弱口令黑名单、首次登录强制改密|弱口令|
|**操作审计**|使用若依 `@Log` 注解，记录操作日志到 MySQL|审计追溯|



> 若依已支持 CSRF、XSS 过滤、SQL 参数化等基础安全，直接沿用。
> 
> 



---



## 八、部署架构（最小化方案）



采用若依推荐的 **Docker Compose** 一键部署，所有中间件容器化：



```Plain Text
┌─────────────────────────────────────────────────────┐
│                    Nginx（HTTPS）                    │
└─────────────────────┬───────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────┐
│           Spring Boot App（若依后端） × 2           │
│           （JVM 堆 2G，连接池 HikariCP）            │
└─────────────────────┬───────────────────────────────┘
                      │
┌──────────┬──────────┼──────────┬──────────┬────────┐
│ MySQL    │  Redis   │    ES    │  Neo4j   │ MinIO  │
│ (主从)   │ (Sentinel)│ (3节点)  │(集群)   │        │
└──────────┴──────────┴──────────┴──────────┴────────┘
```



- **开发/测试环境**：`docker-compose.yml` 一键拉起所有服务（端口仅限 localhost）。

- **生产环境**：数据库独立部署，应用横向扩展，中间件集群化。

    

> 若依本身支持多环境配置（application\-\{env\}\.yml），可直接使用。
> 
> 



---



## 九、精简后的核心依赖（Maven）



```XML
<!-- 若依基础 -->
<dependency>
    <groupId>com.ruoyi</groupId>
    <artifactId>ruoyi-framework</artifactId>
    <version>${ruoyi.version}</version>
</dependency>

<!-- 扩展组件 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-neo4j</artifactId>
</dependency>
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>8.5.2</version>
</dependency>
<!-- 向量嵌入（本地轻量模型） -->
<dependency>
    <groupId>ai.djl</groupId>
    <artifactId>api</artifactId>
    <version>0.22.0</version>
</dependency>
<!-- 其他工具（POI、Jackson等）沿用若依 -->
```



---



