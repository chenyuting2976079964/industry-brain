#!/bin/bash
# restart_mysql_with_remote_access.sh - 重启 MySQL 容器以应用远程访问配置
# 注意：本脚本保留现有数据卷，不会清除数据

set -e

echo "=== 重启 MySQL 容器（保留数据卷） ==="
docker compose up -d mysql

echo "=== 等待 MySQL 初始化完成... ==="
sleep 5

echo "=== 检查容器状态 ==="
docker logs industry_brain_mysql | tail -10

echo ""
echo "=== 远程访问连接信息 ==="
echo "宿主机地址: 你的局域网 IP（本机执行 ipconfig 查看）"
echo "端口: 3307"
echo "用户名: root"
echo "密码: root123"
echo ""
echo "连接示例:"
echo "mysql -h <宿主机IP> -P 3307 -u root -proot123"
echo ""
echo "注意:"
echo "  - 若 MySQL 容器已有历史数据卷，初始化脚本(sql/99-setup-remote-user.sql)不会再次执行"
echo "  - 需在容器内手动授权 root 远程访问: docker exec -it industry_brain_mysql mysql -uroot -proot123"
echo "    CREATE USER IF NOT EXISTS 'root'@'%' IDENTIFIED BY 'root123';"
echo "    GRANT ALL ON industry_brain.* TO 'root'@'%';"
echo "    FLUSH PRIVILEGES;"
