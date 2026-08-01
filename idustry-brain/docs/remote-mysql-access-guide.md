# 其他主机连接 MySQL Docker 容器 - 配置指南

> **实际部署统一使用**：用户名 `root`、密码 `root123`、端口 `3307`。
> 本指南中的账号均以此为准（`remote_user` 已不再创建，见 `idustry-brain-dB/sql/99-setup-remote-user.sql`）。

## 一、目标配置

本指南指导您如何配置 Docker 中的 MySQL，使**局域网内其他设备**或**远程主机**能够连接。

## 二、修改步骤

### 1. 修改 MySQL 绑定地址（已配置完成）

文件：`idustry-brain-dB/config/my-custom.cnf`

```ini
[mysqld]
bind-address = 0.0.0.0      # 监听所有网络接口
skip-name-resolve           # 跳过 DNS 解析，加速连接
max_connections = 1000      # 增加最大连接数
```

该配置文件已通过 `docker-compose.yml` 挂载到容器。

### 2. 启动带远程配置的容器

```bash
# 进入 docker-compose 目录
cd idustry-brain-dB

# 停止旧容器（如有）
docker stop industry_brain_mysql

# 重建容器（应用新配置）
docker-compose up -d
```

> **注意**：首次启动会初始化数据库并自动执行 `sql/` 目录下的 `.sql` 脚本（包括创建远程用户）。

### 3. （可选）开放防火墙端口

#### Windows 宿主机防火墙：

```powershell
# 以管理员身份运行 PowerShell
New-NetFirewallRule -DisplayName "MySQL Remote Access" -Direction Inbound -Protocol TCP -LocalPort 3307 -Action Allow
```

#### Linux 宿主机（如使用 UFW）：

```bash
sudo ufw allow 3307/tcp
sudo ufw reload
```

### 4. 检查 MySQL 远程用户是否正确创建

```bash
# 进入容器
docker exec -it industry_brain_mysql bash

# 登录 MySQL
mysql -u root -p

# 在 MySQL 内执行：
SELECT User, Host FROM mysql.user;
-- 预期输出包含：root | %
```

## 三、连接方式

### 方案 A：SSH 隧道（推荐 - 安全）

```bash
# 本地转发：将本地的 8808 端口的流量转发到服务器的 3307 → 容器 3306
ssh -L 8808:localhost:3307 user@your-server-ip

# 连接 localhost:8808（实际到达的是 MySQL 的 3307）
mysql -h 127.0.0.1 -P 8808 -u root -proot123
```

### 方案 B：直接 TCP 连接（局域网内可用）

```
主机 IP: <您的宿主机局域网IP>（如 192.168.1.100）
端口: 3307
用户名: root
密码: root123
```

**连接示例：**
```bash
mysql -h 192.168.1.100 -P 3307 -u root -proot123
```

### 方案 C：云端访问（公网 IP + 安全组）

如果您在云服务器上运行：
1. 在云服务商控制台的安全组/防火墙上放行 TCP 3307
2. 确保您的实例有公网 IP
3. 使用公网 IP + 3307 端口连接

⚠️ **重要警告**：直接暴露 MySQL 到公网存在严重安全风险！建议：
- 使用强密码
- 仅信任特定 IP 段（将 `%` 替换为具体 CIDR）
- 使用 SSH 隧道或 VPN 作为第一道防护
- 定期轮换密码

## 四、常见问题解决

### Q1: 连接被拒绝 (Connection refused)

**可能原因：**
- 容器未运行：`docker ps` 检查状态
- 防火墙拦截：检查宿主机防火墙设置
- 端口冲突：3307 端口已被占用，修改 docker-compose.yml 中的 ports 映射

**检查命令：**
```bash
docker ps -a                  # 查看容器状态
netstat -an | grep 3307       # Windows: netstat -ano | findstr 3307
docker logs industry_brain_mysql # 查看启动日志
```

### Q2: 访问被拒绝 (Access denied for user)

```bash
# 进入容器后修复用户权限
docker exec -it industry_brain_mysql bash
mysql -u root -p

GRANT ALL PRIVILEGES ON industry_brain.* TO 'root'@'%';
FLUSH PRIVILEGES;
```

### Q3: Host 'xxx' is not allowed to connect

```sql
# 查看用户权限
SELECT User, Host FROM mysql.user WHERE User = 'root';

# 修改 host 为 %（任意主机）
UPDATE mysql.user SET Host = '%' WHERE User = 'root' AND Host = 'localhost';
FLUSH PRIVILEGES;
```

## 五、安全最佳实践

| 风险项 | 当前设置 | 建议改进 |
|--------|---------|---------|
| 认证协议 | mysql_native_password | 可使用 caching_sha2_password（需客户端兼容） |
| 远程用户权限 | ALL PRIVILEGES | 按需缩小为 SELECT/INSERT/UPDATE 等最小权限 |
| Root 远程访问 | 启用（root@'%'） | 局域网可信环境可用；公网请改用专用低权限账号并设强密码 |
| 密码强度 | root123 | 生产环境使用更长随机密码 |
| 连接加密 | 无TLS | 启用 `--ssl-mode` 和 SSL 证书 |

### 🔒 更安全的替代方案：使用特定 IP 白名单

如果您知道远程主机的固定 IP，可以将远程用户限制为：

```sql
CREATE USER 'remote_user'@'192.168.1.50' IDENTIFIED BY 'StrongPassword123!';
GRANT ALL PRIVILEGES ON industry_brain.* TO 'remote_user'@'192.168.1.50';
```

只允许从指定 IP 连接，比 `'%'` 更安全。
