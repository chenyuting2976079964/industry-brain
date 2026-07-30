-- ============================================================
-- 初始化脚本：创建远程访问用户
-- 注意：此脚本在 MySQL 容器启动时自动执行（如果 data volume 为空）
-- 如已存在数据，需手动进入容器执行
-- ============================================================

-- 重要说明：实际部署时，请通过环境变量 MYSQL_REMOTE_USER_PASSWORD 修改密码
-- 或在容器启动后通过以下命令重置：
--   ALTER USER 'remote_user'@'%' IDENTIFIED BY '新密码';

-- 创建远程访问专用用户（不建议直接使用 root 远程登录）
CREATE USER IF NOT EXISTS 'remote_user'@'%' IDENTIFIED BY 'RemotePass123!';
GRANT ALL PRIVILEGES ON industry_brain.* TO 'remote_user'@'%' WITH GRANT OPTION;

-- 验证用户是否创建成功
SELECT User, Host FROM mysql.user WHERE User IN ('root', 'remote_user');
