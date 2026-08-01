-- ============================================================
-- V3: 重构 investment_recommendation 表为三维度算法 v2
-- 变更说明:
--   1. 删除旧表并重建（字段语义不兼容，直接重建更安全）
--   2. 三维度字段重命名: tech_match_score → business_fit_score 等
--   3. 移除死字段: active_score, driving_potential_score, chain_coverage, distance_level, deleted_at
--   4. 新增字段: rating, match_reasons, demand_province
--   5. status CHECK 增加 'wishlist' 值
--   6. enterprise_id 改为 NOT NULL, region_id 改为可空
-- ============================================================

-- 备份旧表
CREATE TABLE IF NOT EXISTS investment_recommendation_backup_v2 AS
SELECT * FROM investment_recommendation WHERE 1=1;

-- 删除旧表及依赖
DROP TABLE IF EXISTS investment_recommendation;

-- 创建新表
CREATE TABLE investment_recommendation (
    id                      BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',

    -- 核心关联
    enterprise_id           BIGINT NOT NULL COMMENT '目标企业ID',
    target_node_id          BIGINT NOT NULL COMMENT '产业链节点ID（弱链/缺链/补链）',
    region_id               BIGINT DEFAULT NULL COMMENT '区域ID（可选）',

    -- 三维度得分（与算法维度一一对应，语义明确）
    business_fit_score      DECIMAL(5,2) DEFAULT NULL COMMENT '业务契合度（维度1，权重50%）',
    scale_strength_score    DECIMAL(5,2) DEFAULT NULL COMMENT '规模实力（维度2，权重35%）',
    regional_synergy_score  DECIMAL(5,2) DEFAULT NULL COMMENT '地域协同度（维度3，权重15%）',

    -- 综合评分
    total_score             DECIMAL(5,2) DEFAULT NULL COMMENT '综合得分 0-100',
    rating                  VARCHAR(2) DEFAULT NULL COMMENT '评级 A/B/C',
    match_reasons           VARCHAR(500) DEFAULT NULL COMMENT '匹配理由/详情文本',

    -- 辅助计算字段
    demand_province         VARCHAR(10) DEFAULT NULL COMMENT '需求方省份编码（regionCode前2位）',

    -- 状态管理
    status                  VARCHAR(20) DEFAULT 'pending' COMMENT '状态: pending/contacted/signed/wishlist',
    recommendation_date     DATE DEFAULT NULL COMMENT '推荐日期',

    -- 审计字段
    is_deleted              TINYINT DEFAULT 0 COMMENT '软删除 0=正常 1=已删',
    created_at              DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at              DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- 索引
    INDEX idx_ent_node (enterprise_id, target_node_id),
    INDEX idx_total (total_score DESC),
    INDEX idx_status (status),
    INDEX idx_province (demand_province),

    -- 外键约束
    CONSTRAINT fk_rec_ent FOREIGN KEY (enterprise_id) REFERENCES enterprise(id) ON DELETE CASCADE,
    CONSTRAINT fk_rec_node FOREIGN KEY (target_node_id) REFERENCES chain_node(id) ON DELETE CASCADE,
    CONSTRAINT fk_rec_region FOREIGN KEY (region_id) REFERENCES region(id) ON DELETE SET NULL,
    CONSTRAINT chk_rec_status CHECK (status IN ('pending', 'contacted', 'signed', 'wishlist'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='招商推荐匹配记录表（v2 三维度算法）';
