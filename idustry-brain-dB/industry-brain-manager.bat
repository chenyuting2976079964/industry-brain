@echo off
chcp 65001 >nul
title 企开得胜 - 中间件启动器

cd /d %~dp0

echo ========================================
echo   企开得胜 Docker 中间件管理
echo ========================================
echo.
echo   MySQL  :3307  ? Redis:6379  ? ES:9200
echo   Neo4j  :7687  ? MinIO:9000(API) / 9001(控制台)
echo.

:menu
echo.
echo 请选择操作：
echo  1. 启动所有服务
echo  2. 停止所有服务
echo  3. 查看运行状态
echo  4. 重启 MySQL（仅重启 MySQL）
echo  5. 查看日志
echo  0. 退出
echo.
set /p choice="选择（回车=1）: "

if "%choice%"=="" set choice=1

if "%choice%"=="1" goto start_all
if "%choice%"=="2" goto stop_all
if "%choice%"=="3" goto status
if "%choice%"=="4" goto restart_mysql
if "%choice%"=="5" goto logs
if "%choice%"=="0" goto end
echo 无效选项，请重新输入
echo.
goto menu

:start_all
echo.
echo [1/5] 检查 Docker 运行状态...
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo 错误：Docker 未运行！请先启动 Docker Desktop。
    pause
    goto end
)

echo [2/5] 启动所有容器...
docker compose up -d

echo [3/5] 等待服务初始化完成（约30秒）...
timeout /t 30 /nobreak >nul

echo [4/5] 检查各服务状态...
docker ps -a --format "table {{.Names}}\t{{.Status}}" | findstr /i "industry_brain"

echo.
echo [5/5] 服务访问信息
echo ========================================
echo  MySQL   ? root/root123    ? localhost:3307
echo  Redis   ? 无密码          ? localhost:6379
echo  ES       ? 无验证          ? localhost:9200
echo  Neo4j  ? neo4j/neo4j123  ? bolt://localhost:7687 / http://localhost:7474
echo  MinIO  ? minioadmin/...  ? http://localhost:9001
echo ========================================
echo.
echo 内网其他人连接时使用本机的局域网IP替换 localhost 即可
echo.
pause
goto end

:stop_all
echo 停止所有容器...
docker compose stop
echo 已停止
pause
goto end

:status
cls
echo 当前运行状态：
echo.
docker ps -a --format "table {{.Names}}\t{{.Image}}\t{{.Ports}}\t{{.Status}}"
echo.
pause
goto end

:restart_mysql
echo 重启 MySQL...
docker compose restart mysql
echo 等待 10 秒...
timeout /t 10 /nobreak >nul
docker ps --filter name=industry_brain_mysql --format "{{.Status}}"
echo 完成
pause
goto end

:logs
echo SE当前可用的日志：
echo  1. MySQL    2. Redis    3. ES    4. Neo4j    5. MinIO
set /p log_choice="查看哪个服务的日志？ "
if "%log_choice%"=="1" docker logs -n 30 industry_brain_mysql
if "%log_choice%"=="2" docker logs -n 30 industry_brain_redis
if "%log_choice%"=="3" docker logs -n 30 industry_brain_es
if "%log_choice%"=="4" docker logs -n 30 industry_brain_neo4j
if "%log_choice%"=="5" docker logs -n 30 industry_brain_minio
echo.
pause
goto end

:end
echo 再见！
timeout /t 2 /nobreak >nul
