-- ============================================================
-- 迁移脚本：为enterprise表添加enterprise_type字段（生产环境兼容版）
-- 版本号：V2.1__add_enterprise_type_to_enterprise.sql
-- 依赖：init.sql已创建新库时的表结构；本脚本用于存量数据库升级
-- 适用：MySQL 5.7+ / 8.0+
-- ============================================================

-- 检查列是否存在，不存在则添加
SET @column_exists = SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'enterprise'
    AND COLUMN_NAME = 'enterprise_type';

-- 如果列不存在，则添加该列
SET @sql = IF(@column_exists = 0,
    'ALTER TABLE enterprise
        ADD COLUMN enterprise_type VARCHAR(64) NULL COMMENT ''企业类型：龙头企业/专精特新/高新技术企业/科技型中小企业/其他'',
        ADD INDEX idx_enterprise_type (enterprise_type)',
    SELECT ''');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 为旧版本MySQL（CHECK约束不生效）添加触发器保证数据完整性（可选）
-- MySQL 8.0.16+ 支持真正的CHECK约束，以下触发器适用于所有版本
SET @trigger_exists = SELECT COUNT(*) FROM information_schema.TRIGGERS
    WHERE TRIGGER_NAME = 'before_enterprise_insert_update_type'
    AND EVENT_OBJECT_TABLE = 'enterprise';

SET @sql = IF(@trigger_exists = 0,
'CREATE TRIGGER before_enterprise_insert_update_type BEFORE INSERT ON enterprise
 FOR EACH ROW BEGIN
   SET NEW.enterprise_type = IFNULL(NEW.enterprise_type, '''');
   -- 允许NULL值或空字符串，应用层校验更灵活
 END;

CREATE TRIGGER before_enterprise_update_type BEFORE UPDATE ON enterprise
 FOR EACH ROW BEGIN
   SET NEW.enterprise_type = IFNULL(NEW.enterprise_type, '''');
 END;',
 SELECT '');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 初始化存量数据：未设置类型的企业默认为'其他'
UPDATE enterprise SET enterprise_type = '其他' WHERE enterprise_type IS NULL;

-- 验证：显示类型分布
SELECT enterprise_type, COUNT(*) as cnt FROM enterprise GROUP BY enterprise_type;

-- ============================================================
-- 回滚脚本（如需恢复）
-- ============================================================
-- DROP TRIGGER IF EXISTS before_enterprise_insert_update_type;
-- DROP TRIGGER IF EXISTS before_enterprise_update_type;
-- ALTER TABLE enterprise DROP INDEX idx_enterprise_type;
-- ALTER TABLE enterprise DROP COLUMN enterprise_type;
