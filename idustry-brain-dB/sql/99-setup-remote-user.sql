-- ============================================================
-- 初始化脚本：确保 root 可远程访问
-- 注意：此脚本仅在 MySQL 容器"数据卷为空"（首次初始化）时自动执行
-- 如已有数据卷，需手动进入容器执行：docker exec -it industry_brain_mysql mysql -uroot -proot123
-- ============================================================

-- 确保 root 支持从任意主机远程访问（docker-compose 已通过 MYSQL_ROOT_PASSWORD 创建 root）
-- 若 root@'%' 已存在，以下语句是幂等的，不会覆盖现有密码
CREATE USER IF NOT EXISTS 'root'@'%' IDENTIFIED BY 'root123';
GRANT ALL PRIVILEGES ON industry_brain.* TO 'root'@'%' WITH GRANT OPTION;
FLUSH PRIVILEGES;

-- 验证用户
SELECT User, Host FROM mysql.user WHERE User IN ('root', 'remote_user');
