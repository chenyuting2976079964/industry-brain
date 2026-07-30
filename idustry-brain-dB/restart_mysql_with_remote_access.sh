#!/bin/bash
# restart_mysql_with_remote_access.sh - 重启 MySQL 容器以应用远程访问配置

set -e

echo "=== 停止现有 MySQL 容器 ==="
docker stop industry_brain_mysql 2>/dev/null || echo "容器未运行，跳过..."

echo "=== 删除现有数据卷（谨慎：这将清除所有数据库数据！） ==="
# 如果要保留数据，请注释或删除下一行
docker volume rm industry_brain_mysql_data 2>/dev/null || echo "数据卷不存在，跳过..."

echo "=== 启动新的 MySQL 容器（含远程访问配置） ==="
docker-compose up -d mysql

echo "=== 等待 MySQL 初始化完成... ==="
sleep 5

echo "=== 检查容器状态 ==="
docker logs industry_brain_mysql | tail -10

echo ""
echo "=== 远程访问连接信息 ==="
echo "宿主机地址: localhost (或你的外网 IP)"
echo "端口: 3307"
echo "用户名: remote_user"
echo "密码: RemotePass123!"
echo ""
echo "连接示例:"
echo "mysql -h 127.0.0.1 -P 3307 -u remote_user -p"
echo "  # 或直接使用主机 IP:"
echo "mysqlh <宿主机IP> -P 3307 -u remote_user -p"
