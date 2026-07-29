-- ============================================================
-- 产业大脑平台 - 数据库建表脚本
-- 版本: v2.1
-- 数据库: industry_brain
-- 引擎: MySQL 8.0 + InnoDB, 字符集 utf8mb4
-- ============================================================

CREATE DATABASE IF NOT EXISTS industry_brain
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE industry_brain;

-- ============================================================
-- 第一批：基础配置表
-- ============================================================

-- 表1：区域表（region）
CREATE TABLE region (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '区域ID',
    name            VARCHAR(100) NOT NULL COMMENT '区域名称',
    code            VARCHAR(50) NOT NULL UNIQUE COMMENT '区域编码（GB/T 2260 国标）',
    parent_id       BIGINT DEFAULT NULL COMMENT '上级区域ID',
    is_deleted      TINYINT DEFAULT 0 COMMENT '软删除：0正常/1已删除',
    deleted_at      DATETIME DEFAULT NULL COMMENT '删除时间',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_parent_id (parent_id),
    CONSTRAINT fk_region_parent FOREIGN KEY (parent_id) REFERENCES region(id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='区域表';

-- 表2：园区表（industrial_park）
CREATE TABLE industrial_park (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '园区ID',
    name            VARCHAR(128) NOT NULL COMMENT '园区名称',
    region_id       BIGINT NOT NULL COMMENT '所属区域ID',
    park_level      VARCHAR(20) DEFAULT NULL COMMENT '园区级别：国家级/省级/市级/区级',
    address         VARCHAR(255) DEFAULT NULL COMMENT '园区地址',
    description     TEXT DEFAULT NULL COMMENT '园区简介',
    park_type       VARCHAR(30) DEFAULT NULL COMMENT '园区类型：核心园区/产业聚集区/特色园区/孵化园区',
    enterprise_count INT DEFAULT 0 COMMENT '企业数量',
    patent_count    INT DEFAULT 0 COMMENT '专利数量',
    high_value_patent_count INT DEFAULT 0 COMMENT '高价值专利数量',
    dominant_industries VARCHAR(500) DEFAULT NULL COMMENT '主导产业',
    key_firms       VARCHAR(500) DEFAULT NULL COMMENT '重点企业',
    chain_coverage  VARCHAR(500) DEFAULT NULL COMMENT '产业链覆盖情况',
    is_deleted      TINYINT DEFAULT 0 COMMENT '软删除',
    deleted_at      DATETIME DEFAULT NULL COMMENT '删除时间',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_region_id (region_id),
    INDEX idx_name (name),
    CONSTRAINT fk_park_region FOREIGN KEY (region_id) REFERENCES region(id)
        ON DELETE CASCADE,
    CONSTRAINT chk_park_level CHECK (park_level IS NULL OR park_level IN ('国家级', '省级', '市级', '区级')),
    CONSTRAINT chk_park_type CHECK (park_type IS NULL OR park_type IN ('核心园区', '产业聚集区', '特色园区', '孵化园区'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='园区表';

-- 表3：产业链表（industry_chain）
CREATE TABLE industry_chain (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '产业链ID',
    name            VARCHAR(100) NOT NULL COMMENT '产业链名称',
    code            VARCHAR(50) NOT NULL UNIQUE COMMENT '产业链编码',
    is_deleted      TINYINT DEFAULT 0 COMMENT '软删除：0正常/1已删除',
    deleted_at      DATETIME DEFAULT NULL COMMENT '删除时间',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产业链表';

-- 表4：产业链节点表（chain_node）
CREATE TABLE chain_node (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '节点ID',
    chain_id        BIGINT NOT NULL COMMENT '所属产业链ID',
    name            VARCHAR(100) NOT NULL COMMENT '节点名称',
    layer           VARCHAR(10) DEFAULT NULL COMMENT '所属层级：up上游/mid中游/down下游',
    description     VARCHAR(200) DEFAULT NULL COMMENT '节点描述',
    firms           VARCHAR(500) DEFAULT NULL COMMENT '代表企业（逗号分隔）',
    parent_id       BIGINT DEFAULT NULL COMMENT '父节点ID',
    sort_order      INT DEFAULT 0 COMMENT '排序序号',
    is_deleted      TINYINT DEFAULT 0 COMMENT '软删除：0正常/1已删除',
    deleted_at      DATETIME DEFAULT NULL COMMENT '删除时间',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_chain_id (chain_id),
    INDEX idx_parent_id (parent_id),
    CONSTRAINT fk_node_chain FOREIGN KEY (chain_id) REFERENCES industry_chain(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_node_parent FOREIGN KEY (parent_id) REFERENCES chain_node(id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产业链节点表';

-- 表5：节点关系表（chain_node_link）
CREATE TABLE chain_node_link (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关系ID',
    source_node_id      BIGINT NOT NULL COMMENT '源节点ID',
    target_node_id      BIGINT NOT NULL COMMENT '目标节点ID',
    relation_type       VARCHAR(20) NOT NULL COMMENT '关系类型：SUPPLIES/DEMANDS',
    is_deleted          TINYINT DEFAULT 0 COMMENT '软删除',
    deleted_at          DATETIME DEFAULT NULL COMMENT '删除时间',
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_node_link (source_node_id, target_node_id, relation_type),
    INDEX idx_target (target_node_id),
    CONSTRAINT fk_link_source FOREIGN KEY (source_node_id) REFERENCES chain_node(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_link_target FOREIGN KEY (target_node_id) REFERENCES chain_node(id)
        ON DELETE CASCADE,
    CONSTRAINT chk_link_type CHECK (relation_type IN ('SUPPLIES', 'DEMANDS'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='节点关系表';

-- ============================================================
-- 第二批：用户模块
-- ============================================================

-- 表6：用户表（user）
CREATE TABLE user (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username        VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password        VARCHAR(128) NOT NULL COMMENT '密码（BCrypt加密）',
    real_name       VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
    email           VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    phone           VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    region_id       BIGINT DEFAULT NULL COMMENT '所属区域ID',
    role_code       VARCHAR(30) DEFAULT 'OPERATOR' COMMENT '角色：ADMIN/OPERATOR/VIEWER',
    status          TINYINT DEFAULT 1 COMMENT '状态：1正常/0禁用',
    login_fail_count INT DEFAULT 0 COMMENT '登录失败次数',
    lock_until      DATETIME DEFAULT NULL COMMENT '锁定到期时间',
    password_updated_at DATETIME DEFAULT NULL COMMENT '密码最后修改时间',
    is_deleted      TINYINT DEFAULT 0 COMMENT '软删除',
    deleted_at      DATETIME DEFAULT NULL COMMENT '删除时间',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_region_id (region_id),
    CONSTRAINT fk_user_region FOREIGN KEY (region_id) REFERENCES region(id)
        ON DELETE SET NULL,
    CONSTRAINT chk_user_status CHECK (status IN (0, 1)),
    CONSTRAINT chk_user_role CHECK (role_code IN ('ADMIN', 'OPERATOR', 'VIEWER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 表7：用户Token表（user_token）
CREATE TABLE user_token (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'TokenID',
    user_id         BIGINT NOT NULL COMMENT '用户ID',
    token           VARCHAR(512) NOT NULL COMMENT 'JWT Token',
    token_type      VARCHAR(20) DEFAULT 'ACCESS' COMMENT 'Token类型',
    expires_at      DATETIME NOT NULL COMMENT '过期时间',
    is_revoked      TINYINT DEFAULT 0 COMMENT '是否已撤销（黑名单）',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_token (token(255)),
    INDEX idx_expires (expires_at),
    CONSTRAINT fk_token_user FOREIGN KEY (user_id) REFERENCES user(id)
        ON DELETE CASCADE,
    CONSTRAINT chk_token_type CHECK (token_type IN ('ACCESS', 'REFRESH'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户Token表';

-- ============================================================
-- 第三批：核心主数据表
-- ============================================================

-- 表8：企业表（enterprise）
CREATE TABLE enterprise (
    id                      BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '企业ID',
    external_id             VARCHAR(50) DEFAULT NULL COMMENT '外部数据源企业ID',
    name                    VARCHAR(200) NOT NULL COMMENT '企业名称',
    description             TEXT DEFAULT NULL COMMENT '企业简介/描述',
    unified_code            VARCHAR(18) DEFAULT NULL COMMENT '统一社会信用代码',
    legal_representative    VARCHAR(50) DEFAULT NULL COMMENT '法定代表人',
    registered_capital      VARCHAR(50) DEFAULT NULL COMMENT '注册资本',
    establishment_date      DATE DEFAULT NULL COMMENT '成立日期',
    business_status         VARCHAR(20) DEFAULT NULL COMMENT '经营状态',
    registered_address      VARCHAR(255) DEFAULT NULL COMMENT '注册地址（城市级）',
    contact_phone           VARCHAR(50) DEFAULT NULL COMMENT '联系电话',
    region_id               BIGINT DEFAULT NULL COMMENT '所属区域ID',
    park_id                 BIGINT DEFAULT NULL COMMENT '所属园区ID',
    enterprise_type         VARCHAR(64) DEFAULT NULL COMMENT '企业类型：龙头企业/专精特新/高新技术企业/科技型中小企业/其他',
    employee_scale          VARCHAR(30) DEFAULT NULL COMMENT '人员规模',
    is_local                TINYINT DEFAULT 0 COMMENT '是否本地企业：1是/0否',
    data_version            DATETIME DEFAULT NULL COMMENT '数据版本时间',
    is_deleted              TINYINT DEFAULT 0 COMMENT '软删除',
    deleted_at              DATETIME DEFAULT NULL COMMENT '删除时间',
    created_at              DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at              DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_unified_code (unified_code),
    INDEX idx_name (name),
    INDEX idx_external_id (external_id),
    INDEX idx_region_id (region_id),
    INDEX idx_park_id (park_id),
    CONSTRAINT fk_enterprise_region FOREIGN KEY (region_id) REFERENCES region(id)
        ON DELETE SET NULL,
    CONSTRAINT fk_enterprise_park FOREIGN KEY (park_id) REFERENCES industrial_park(id)
        ON DELETE SET NULL,
    CONSTRAINT chk_enterprise_type CHECK (enterprise_type IS NULL OR enterprise_type IN ('龙头企业', '专精特新', '高新技术企业', '科技型中小企业', '其他'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业表';

-- 表9：企业别名表（enterprise_alias）
CREATE TABLE enterprise_alias (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '别名ID',
    enterprise_id   BIGINT NOT NULL COMMENT '企业ID',
    alias_name      VARCHAR(200) NOT NULL COMMENT '别名/简称/曾用名/专利申请用名',
    alias_type      VARCHAR(30) DEFAULT 'short_name' COMMENT '别名类型',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_enterprise_id (enterprise_id),
    INDEX idx_alias_name (alias_name),
    CONSTRAINT fk_alias_enterprise FOREIGN KEY (enterprise_id) REFERENCES enterprise(id)
        ON DELETE CASCADE,
    CONSTRAINT chk_alias_type CHECK (alias_type IN ('short_name', 'full_name', 'former_name', 'patent_applicant'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业别名表';

-- 表10：专利表（patent）
CREATE TABLE patent (
    id                      BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '专利ID',
    patent_number           VARCHAR(50) NOT NULL UNIQUE COMMENT '专利号',
    patent_name             VARCHAR(500) NOT NULL COMMENT '专利名称',
    applicant               VARCHAR(200) DEFAULT NULL COMMENT '申请人（原始文本）',
    applicant_enterprise_id BIGINT DEFAULT NULL COMMENT '关联的企业ID',
    ipc_main                VARCHAR(50) DEFAULT NULL COMMENT 'IPC主分类',
    patent_type             VARCHAR(20) DEFAULT NULL COMMENT '专利类型',
    application_date        DATE DEFAULT NULL COMMENT '申请日',
    legal_status            VARCHAR(30) DEFAULT NULL COMMENT '法律状态',
    abstract_text           TEXT DEFAULT NULL COMMENT '专利摘要/描述',
    estimated_value         VARCHAR(50) DEFAULT NULL COMMENT '专利估值（万元）',
    maintenance_years       INT DEFAULT NULL COMMENT '维持年限',
    data_source             VARCHAR(20) DEFAULT 'CNIPA' COMMENT '数据来源',
    region_id               BIGINT DEFAULT NULL COMMENT '所属区域ID',
    is_deleted              TINYINT DEFAULT 0 COMMENT '软删除',
    deleted_at              DATETIME DEFAULT NULL COMMENT '删除时间',
    created_at              DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at              DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_applicant_enterprise_id (applicant_enterprise_id),
    INDEX idx_ipc_main (ipc_main),
    INDEX idx_patent_number (patent_number),
    INDEX idx_region_id (region_id),
    CONSTRAINT fk_patent_enterprise FOREIGN KEY (applicant_enterprise_id) REFERENCES enterprise(id)
        ON DELETE SET NULL,
    CONSTRAINT fk_patent_region FOREIGN KEY (region_id) REFERENCES region(id)
        ON DELETE SET NULL,
    CONSTRAINT chk_patent_type CHECK (patent_type IS NULL OR patent_type IN ('发明', '实用新型', '外观设计'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专利表';

-- 表19：企业技术标签表（enterprise_tag）
CREATE TABLE enterprise_tag (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '标签ID',
    enterprise_id   BIGINT NOT NULL COMMENT '企业ID',
    tag_name        VARCHAR(100) NOT NULL COMMENT '技术标签名称',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_ent_tag (enterprise_id, tag_name),
    INDEX idx_tag_name (tag_name),
    CONSTRAINT fk_tag_enterprise FOREIGN KEY (enterprise_id) REFERENCES enterprise(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业技术标签表';

-- 表20：企业技术优势表（enterprise_advantage）
CREATE TABLE enterprise_advantage (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '优势ID',
    enterprise_id   BIGINT NOT NULL COMMENT '企业ID',
    advantage_name  VARCHAR(100) NOT NULL COMMENT '技术优势名称',
    advantage_count VARCHAR(20) DEFAULT NULL COMMENT '相关专利数量',
    description     VARCHAR(500) DEFAULT NULL COMMENT '优势描述',
    sort_order      INT DEFAULT 0 COMMENT '排序',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_ent_adv (enterprise_id, advantage_name),
    INDEX idx_advantage_name (advantage_name),
    CONSTRAINT fk_adv_enterprise FOREIGN KEY (enterprise_id) REFERENCES enterprise(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业技术优势表';

-- 表21：企业招商评估结果表（enterprise_evaluation）
CREATE TABLE enterprise_evaluation (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评估ID',
    enterprise_id   BIGINT NOT NULL UNIQUE COMMENT '企业ID（一对一）',
    eval_level      CHAR(1) DEFAULT NULL COMMENT '评级：A/B/C',
    eval_label      VARCHAR(20) DEFAULT NULL COMMENT '评级标签：强烈推荐/推荐/值得关注',
    tech_match      INT DEFAULT NULL COMMENT '技术匹配度 0-1000',
    patent_strength INT DEFAULT NULL COMMENT '专利实力评分 0-100',
    driving_potential INT DEFAULT NULL COMMENT '产业带动潜力 0-100',
    invest_type     VARCHAR(100) DEFAULT NULL COMMENT '招商方式',
    urgency         VARCHAR(10) DEFAULT NULL COMMENT '紧急程度：高/中/低',
    risk_level      VARCHAR(20) DEFAULT NULL COMMENT '风险等级',
    policy_category VARCHAR(20) DEFAULT NULL COMMENT '政策分类',
    suggestion      TEXT DEFAULT NULL COMMENT '招商建议',
    land_suggest    VARCHAR(200) DEFAULT NULL COMMENT '建议落地园区',
    weakness        VARCHAR(200) DEFAULT NULL COMMENT '技术短板',
    weakness_detail TEXT DEFAULT NULL COMMENT '短板详情',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT fk_eval_enterprise FOREIGN KEY (enterprise_id) REFERENCES enterprise(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业招商评估结果表';

-- ============================================================
-- 第四批：关联映射表
-- ============================================================

-- 表11：企业-节点关联表（enterprise_node）
CREATE TABLE enterprise_node (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
    enterprise_id       BIGINT NOT NULL COMMENT '企业ID',
    node_id             BIGINT NOT NULL COMMENT '产业链节点ID',
    match_type          VARCHAR(20) DEFAULT 'manual' COMMENT '匹配方式',
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_enterprise_node (enterprise_id, node_id),
    INDEX idx_node_id (node_id),
    CONSTRAINT fk_en_enterprise FOREIGN KEY (enterprise_id) REFERENCES enterprise(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_en_node FOREIGN KEY (node_id) REFERENCES chain_node(id)
        ON DELETE CASCADE,
    CONSTRAINT chk_en_match CHECK (match_type IN ('auto', 'manual'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业-节点关联表';

-- 表12：专利-节点关联表（patent_node）
CREATE TABLE patent_node (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
    patent_id           BIGINT NOT NULL COMMENT '专利ID',
    node_id             BIGINT NOT NULL COMMENT '产业链节点ID',
    match_type          VARCHAR(20) DEFAULT 'manual' COMMENT '匹配方式',
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_patent_node (patent_id, node_id),
    INDEX idx_node_id (node_id),
    CONSTRAINT fk_pn_patent FOREIGN KEY (patent_id) REFERENCES patent(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_pn_node FOREIGN KEY (node_id) REFERENCES chain_node(id)
        ON DELETE CASCADE,
    CONSTRAINT chk_pn_match CHECK (match_type IN ('auto', 'manual'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专利-节点关联表';

-- ============================================================
-- 第五批：业务智能计算表
-- ============================================================

-- 表13：节点诊断结果表（node_diagnosis）
CREATE TABLE node_diagnosis (
    id                       BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '诊断ID',
    region_id                BIGINT NOT NULL COMMENT '区域ID',
    node_id                  BIGINT NOT NULL COMMENT '产业链节点ID',
    diagnosis_tag            VARCHAR(10) NOT NULL COMMENT '诊断标签',
    high_value_patent_count  INT DEFAULT 0 COMMENT '高价值专利数量',
    total_patent_count       INT DEFAULT 0 COMMENT '专利总数',
    enterprise_count         INT DEFAULT 0 COMMENT '企业数量',
    patent_density           DECIMAL(5,2) DEFAULT NULL COMMENT '专利密集度百分比',
    diagnosis_basis          TEXT DEFAULT NULL COMMENT '诊断依据说明',
    diagnosis_date           DATE NOT NULL COMMENT '诊断计算日期',
    is_deleted               TINYINT DEFAULT 0 COMMENT '软删除',
    deleted_at               DATETIME DEFAULT NULL COMMENT '删除时间',
    created_at               DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at               DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_region_node_date (region_id, node_id, diagnosis_date),
    INDEX idx_region_id (region_id),
    INDEX idx_diagnosis_tag (diagnosis_tag),
    CONSTRAINT fk_diag_region FOREIGN KEY (region_id) REFERENCES region(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_diag_node FOREIGN KEY (node_id) REFERENCES chain_node(id)
        ON DELETE CASCADE,
    CONSTRAINT chk_diag_tag CHECK (diagnosis_tag IN ('强链', '补链', '弱链', '缺链'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='节点诊断结果表';

-- 表14：招商推荐记录表（investment_recommendation）
CREATE TABLE investment_recommendation (
    id                       BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '推荐ID',
    region_id                BIGINT NOT NULL COMMENT '区域ID',
    target_node_id           BIGINT NOT NULL COMMENT '目标节点ID（弱链/缺链）',
    enterprise_id            BIGINT DEFAULT NULL COMMENT '推荐目标企业ID',
    tech_match_score         DECIMAL(5,2) DEFAULT NULL COMMENT '技术匹配度',
    tech_strength_score      DECIMAL(5,2) DEFAULT NULL COMMENT '技术实力',
    active_score             DECIMAL(5,2) DEFAULT NULL COMMENT '活跃度',
    industry_fit_score       DECIMAL(5,2) DEFAULT NULL COMMENT '产业链适配性',
    driving_potential_score  DECIMAL(5,2) DEFAULT NULL COMMENT '产业带动潜力',
    chain_coverage           DECIMAL(5,2) DEFAULT NULL COMMENT '链条覆盖广度',
    total_score              DECIMAL(5,2) DEFAULT NULL COMMENT '综合匹配度总分',
    distance_level           VARCHAR(10) DEFAULT NULL COMMENT '区域距离',
    status                   VARCHAR(20) DEFAULT 'pending' COMMENT '状态',
    recommendation_date      DATE NOT NULL COMMENT '推荐日期',
    is_deleted               TINYINT DEFAULT 0 COMMENT '软删除',
    deleted_at               DATETIME DEFAULT NULL COMMENT '删除时间',
    created_at               DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at               DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_region_node (region_id, target_node_id),
    INDEX idx_total_score (total_score DESC),
    INDEX idx_status (status),
    CONSTRAINT fk_rec_region FOREIGN KEY (region_id) REFERENCES region(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_rec_node FOREIGN KEY (target_node_id) REFERENCES chain_node(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_rec_enterprise FOREIGN KEY (enterprise_id) REFERENCES enterprise(id)
        ON DELETE SET NULL,
    CONSTRAINT chk_distance CHECK (distance_level IS NULL OR distance_level IN ('本市', '本省', '邻省', '其他')),
    CONSTRAINT chk_rec_status CHECK (status IN ('pending', 'contacted', 'signed'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='招商推荐记录表';

-- 表22：预警表（alert）
CREATE TABLE alert (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '预警ID',
    alert_level     VARCHAR(10) NOT NULL COMMENT '预警级别：high/medium/info',
    title           VARCHAR(200) NOT NULL COMMENT '预警标题',
    description     TEXT DEFAULT NULL COMMENT '预警描述',
    related_node_id BIGINT DEFAULT NULL COMMENT '关联产业链节点ID',
    is_resolved     TINYINT DEFAULT 0 COMMENT '是否已处理：0未处理/1已处理',
    resolved_at     DATETIME DEFAULT NULL COMMENT '处理时间',
    is_deleted      TINYINT DEFAULT 0 COMMENT '软删除',
    deleted_at      DATETIME DEFAULT NULL COMMENT '删除时间',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_alert_level (alert_level),
    INDEX idx_is_resolved (is_resolved),
    INDEX idx_node_id (related_node_id),
    CONSTRAINT fk_alert_node FOREIGN KEY (related_node_id) REFERENCES chain_node(id)
        ON DELETE SET NULL,
    CONSTRAINT chk_alert_level CHECK (alert_level IN ('high', 'medium', 'info'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警表';

-- ============================================================
-- 第六批：系统运维与审计表
-- ============================================================

-- 表15：附件表（attachment）
CREATE TABLE attachment (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '附件ID',
    biz_type        VARCHAR(30) NOT NULL COMMENT '业务类型',
    biz_id          BIGINT NOT NULL COMMENT '关联业务ID',
    file_name       VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_url        VARCHAR(500) NOT NULL COMMENT '对象存储URL（MinIO）',
    file_size       BIGINT DEFAULT NULL COMMENT '文件大小（字节）',
    file_type       VARCHAR(50) DEFAULT NULL COMMENT 'MIME类型',
    sort_order      INT DEFAULT 0 COMMENT '排序序号',
    is_deleted      TINYINT DEFAULT 0 COMMENT '软删除',
    deleted_at      DATETIME DEFAULT NULL COMMENT '删除时间',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_biz (biz_type, biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='附件表';

-- 表16：招商报告表（report）
CREATE TABLE report (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '报告ID',
    title           VARCHAR(200) NOT NULL COMMENT '报告标题',
    report_type     VARCHAR(30) NOT NULL COMMENT '报告类型：企业画像报告/招商推荐清单/产业链诊断报告/补链建议方案/综合报告',
    scope           VARCHAR(50) DEFAULT NULL COMMENT '报告范围',
    format          VARCHAR(10) DEFAULT 'PDF' COMMENT '导出格式：PDF/XLSX',
    file_id         BIGINT DEFAULT NULL COMMENT '关联附件ID',
    status          VARCHAR(20) DEFAULT 'pending' COMMENT '状态',
    region_id       BIGINT DEFAULT NULL COMMENT '所属区域ID',
    user_id         BIGINT NOT NULL COMMENT '创建人ID',
    is_deleted      TINYINT DEFAULT 0 COMMENT '软删除',
    deleted_at      DATETIME DEFAULT NULL COMMENT '删除时间',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_region_id (region_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_report_type (report_type),
    CONSTRAINT fk_report_region FOREIGN KEY (region_id) REFERENCES region(id)
        ON DELETE SET NULL,
    CONSTRAINT fk_report_user FOREIGN KEY (user_id) REFERENCES user(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_report_file FOREIGN KEY (file_id) REFERENCES attachment(id)
        ON DELETE SET NULL,
    CONSTRAINT chk_report_format CHECK (format IN ('PDF', 'XLSX')),
    CONSTRAINT chk_report_status CHECK (status IN ('pending', 'processing', 'done', 'failed')),
    CONSTRAINT chk_report_type CHECK (report_type IN ('企业画像报告', '招商推荐清单', '产业链诊断报告', '补链建议方案', '综合报告'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='招商报告表';

-- 表17：操作日志表（operation_log）
CREATE TABLE operation_log (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    user_id          BIGINT DEFAULT NULL COMMENT '操作用户ID',
    region_id        BIGINT DEFAULT NULL COMMENT '所属区域ID',
    operation_type   VARCHAR(50) NOT NULL COMMENT '操作类型',
    operation_desc   VARCHAR(500) DEFAULT NULL COMMENT '操作描述',
    ip_address       VARCHAR(45) DEFAULT NULL COMMENT '客户端IP',
    status           TINYINT DEFAULT 1 COMMENT '操作结果：1成功/0失败',
    created_at       DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_region_id (region_id),
    INDEX idx_created_at (created_at),
    CONSTRAINT fk_log_user FOREIGN KEY (user_id) REFERENCES user(id)
        ON DELETE SET NULL,
    CONSTRAINT fk_log_region FOREIGN KEY (region_id) REFERENCES region(id)
        ON DELETE SET NULL,
    CONSTRAINT chk_log_status CHECK (status IN (0, 1)),
    CONSTRAINT chk_log_type CHECK (operation_type IN ('LOGIN', 'EXPORT', 'DIAGNOSE', 'QUERY'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 表18：数据同步日志表（data_sync_log）
CREATE TABLE data_sync_log (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    sync_type        VARCHAR(20) NOT NULL COMMENT '同步类型：ENTERPRISE/PATENT/NEO4J_GRAPH/ES_INDEX',
    data_source      VARCHAR(20) NOT NULL COMMENT '数据源',
    sync_start_time  DATETIME NOT NULL COMMENT '同步开始时间',
    sync_end_time    DATETIME DEFAULT NULL COMMENT '同步结束时间',
    total_count      INT DEFAULT 0 COMMENT '总处理条数',
    success_count    INT DEFAULT 0 COMMENT '成功条数',
    fail_count       INT DEFAULT 0 COMMENT '失败条数',
    sync_status      VARCHAR(20) DEFAULT 'RUNNING' COMMENT '状态',
    error_msg        TEXT DEFAULT NULL COMMENT '错误信息',
    created_at       DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_sync_type (sync_type),
    INDEX idx_sync_status (sync_status),
    CONSTRAINT chk_sync_status CHECK (sync_status IN ('RUNNING', 'SUCCESS', 'FAILED', 'PARTIAL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据同步日志表';
