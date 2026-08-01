@echo off
chcp 65001 >nul
rem configure_remote_mysql.bat - 配置 MySQL 远程访问（Windows 版）

echo ========================================
echo   产业大脑平台 - MySQL 远程访问配置工具
echo ========================================

cd /d %~dp0

echo.
echo [1] 检查 Docker 是否运行...
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo 错误：Docker 未运行或未安装！请先启动 Docker Desktop。
    pause
    exit /b 1
)
echo Docker 正常。

echo.
echo [2] 重启 MySQL 应用新配置...
docker compose down mysql
docker compose up -d mysql

echo.
echo [3] 等待初始化完成...
timeout /t 5 /nobreak >nul

echo.
echo [4] 查看容器日志确认...
docker logs industry_brain_mysql | findstr /i "grant initialized error"

echo.
echo [5] 添加防火墙放行规则（需管理员权限）...
netsh advfirewall firewall add rule name="industry-brain MySQL 3307" dir=in action=allow protocol=TCP localport=3307

echo.
echo ========================================
echo 远程访问配置已完成！
echo ========================================
echo.
echo 连接信息：
echo   主机：     <您的宿主机局域网IP> （如 192.168.1.100）
echo   端口：     3307
echo   用户名：   root
echo   密码：     root123
echo.
echo 连接示例：
echo   mysql -h 192.168.1.100 -P 3307 -u root -proot123
echo.
echo 注意：本机 MySQL 容器若已有历史数据卷，初始化脚本不会再次执行，
echo       需在容器内手动授权 root 远程访问。参见 sql/99-setup-remote-user.sql。
pause
