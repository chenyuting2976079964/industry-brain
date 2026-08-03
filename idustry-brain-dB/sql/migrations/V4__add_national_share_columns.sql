-- ============================================================
-- 迁移脚本：node_diagnosis 表增加"本地企业全国占比"相关字段
-- 版本号：V4__add_national_share_columns.sql
-- 背景：强弱链诊断从"本地绝对数量"改为"本地企业占全国比例"判定
-- 适用：MySQL 5.7+ / 8.0+
-- ============================================================

-- 检查列是否存在，不存在则添加（幂等，可重复执行）
SET @col1 = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'node_diagnosis'
    AND COLUMN_NAME = 'national_enterprise_count');
SET @col2 = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'node_diagnosis'
    AND COLUMN_NAME = 'local_enterprise_count');
SET @col3 = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'node_diagnosis'
    AND COLUMN_NAME = 'local_enterprise_share');

SET @sql = IF(@col1 + @col2 + @col3 < 3,
    'ALTER TABLE node_diagnosis
        ADD COLUMN national_enterprise_count INT DEFAULT 0 COMMENT ''全国该节点企业数（本地+外地）'',
        ADD COLUMN local_enterprise_count INT DEFAULT 0 COMMENT ''本地该节点企业数'',
        ADD COLUMN local_enterprise_share DECIMAL(6,4) DEFAULT NULL COMMENT ''本地企业全国占比(小数,0~1)''',
    'SELECT 1');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 验证
SHOW COLUMNS FROM node_diagnosis LIKE '%enterprise%';

-- ============================================================
-- 回滚脚本（如需恢复）
-- ============================================================
-- ALTER TABLE node_diagnosis
--   DROP COLUMN local_enterprise_share,
--   DROP COLUMN local_enterprise_count,
--   DROP COLUMN national_enterprise_count;
