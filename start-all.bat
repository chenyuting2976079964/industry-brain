@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion
cd /d "%~dp0"

echo ============================================================
echo   企开得胜 — 一键启动（中间件 + 向量 + 后端 + 前端）
echo ============================================================
echo.

REM ========== 0. 读取 .env（唯一配置源） ==========
if not exist ".env" (
    echo [ERROR] 未找到 .env，请先执行: copy .env.example .env
    exit /b 1
)
for /f "usebackq eol=# tokens=1,* delims==" %%a in (".env") do (
    if not "%%a"=="" set "%%a=%%b"
)

REM ========== 映射为后端 Spring 环境变量 ==========
set "SERVER_PORT=%BACKEND_HOST_PORT%"
set "MYSQL_HOST=127.0.0.1"
set "MYSQL_PORT=%MYSQL_HOST_PORT%"
set "MYSQL_USER=root"
set "MYSQL_PASSWORD=%MYSQL_ROOT_PASSWORD%"
set "REDIS_HOST=127.0.0.1"
set "REDIS_PORT=%REDIS_HOST_PORT%"
set "ELASTICSEARCH_HOST=127.0.0.1"
set "ELASTICSEARCH_PORT=%ES_HOST_PORT%"
set "NEO4J_URI=bolt://127.0.0.1:%NEO4J_BOLT_PORT%"
set "NEO4J_USERNAME=neo4j"
set "NEO4J_PASSWORD=%NEO4J_PASSWORD%"
set "MINIO_ENDPOINT=http://127.0.0.1:%MINIO_API_PORT%"
set "MINIO_ACCESS_KEY=%MINIO_ROOT_USER%"
set "MINIO_SECRET_KEY=%MINIO_ROOT_PASSWORD%"
set "EMBEDDING_ENABLED=true"
set "EMBEDDING_BASE_URL=http://127.0.0.1:8701"
set "EMBEDDING_MODEL=BAAI/bge-small-zh-v1.5"
set "EMBEDDING_DIMENSION=512"

set "DOCKER=docker"
where docker >nul 2>nul || set "DOCKER=C:\Users\28737\AppData\Local\Programs\DockerDesktop\resources\bin\docker.exe"
set "PYTHON=C:\Python\Python313\python.exe"
if not exist "%PYTHON%" set "PYTHON=python"
set "JAVA=C:\java\java25\bin\java.exe"
if not exist "%JAVA%" set "JAVA=java"

if not exist "logs" mkdir logs

REM ========== 1. Docker 中间件 ==========
echo [1/4] 检查中间件容器...
"%DOCKER%" info >nul 2>nul
if errorlevel 1 (
    echo        Docker 引擎未运行，尝试启动 Docker Desktop...
    start "" "C:\Program Files\Docker\Docker\Docker Desktop.exe" 2>nul
    for /l %%i in (1,1,30) do (
        timeout /t 5 /nobreak >nul
        "%DOCKER%" info >nul 2>nul && goto :docker_ready
    )
    echo [ERROR] Docker 引擎启动超时
    exit /b 1
)
:docker_ready
for %%c in (industry_brain_mysql industry_brain_redis industry_brain_es industry_brain_neo4j industry_brain_minio) do (
    "%DOCKER%" ps --format "{{.Names}}" | findstr /x /c:"%%c" >nul || set "NEED_UP=1"
)
if defined NEED_UP (
    echo        启动容器...
    "%DOCKER%" compose up -d
) else (
    echo        容器已在运行
)
echo        等待容器 healthy...
set "NOT_READY=1"
for /l %%i in (1,1,36) do (
    set "BAD=0"
    for %%c in (industry_brain_mysql industry_brain_redis industry_brain_es industry_brain_neo4j industry_brain_minio) do (
        for /f "tokens=*" %%s in ('"%DOCKER%" inspect -f "{{.State.Health.Status}}" %%c 2^>nul') do (
            if not "%%s"=="healthy" set "BAD=1"
        )
    )
    if "!BAD!"=="0" (
        set "NOT_READY="
        goto :docker_ok
    )
    timeout /t 5 /nobreak >nul
)
:docker_ok
if defined NOT_READY (
    echo        [WARN] 部分容器未在 3 分钟内 healthy，请检查 docker compose ps
) else (
    echo        5 个容器全部 healthy
)

REM ========== 2. 向量嵌入服务 8701 ==========
echo [2/4] 检查向量嵌入服务 8701...
netstat -ano | findstr /r /c:"127.0.0.1:8701 .*LISTENING" >nul
if errorlevel 1 (
    echo        启动中...
    start "" /b "%PYTHON%" tools\embedding_service.py > tools\embedding-run.log 2>&1
    for /l %%i in (1,1,20) do (
        timeout /t 2 /nobreak >nul
        curl -s -o nul --max-time 2 http://127.0.0.1:8701/health && goto :emb_ok
    )
    echo        [WARN] 8701 未在 40 秒内就绪
    goto :emb_done
)
echo        已在运行
:emb_ok
:emb_done

REM ========== 3. 后端 8890 ==========
echo [3/4] 检查后端 %SERVER_PORT%...
netstat -ano | findstr /r /c:"127.0.0.1:%SERVER_PORT% .*LISTENING" >nul
if errorlevel 1 (
    echo        启动中...
    start "" /b "%JAVA%" -jar industry-brain-admin\target\industry-brain-admin.jar >> logs\backend-console.log 2>&1
    for /l %%i in (1,1,45) do (
        timeout /t 2 /nobreak >nul
        curl -s -o nul --max-time 2 -X POST http://127.0.0.1:%SERVER_PORT%/api/v1/auth/login -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"123456\"}" && goto :be_ok
    )
    echo        [WARN] 后端未在 90 秒内就绪，查看 logs\backend-console.log
    goto :be_done
)
echo        已在运行
:be_ok
:be_done

REM ========== 4. 前端 8888 ==========
echo [4/4] 检查前端 %FRONTEND_HOST_PORT%...
netstat -ano | findstr /r /c:"127.0.0.1:%FRONTEND_HOST_PORT% .*LISTENING" >nul
if errorlevel 1 (
    echo        启动中...
    pushd industry-brain-web
    start "" /b cmd /c "npx vite --port %FRONTEND_HOST_PORT% --host 0.0.0.0 > ..\logs\frontend-vite.log 2>&1"
    popd
    for /l %%i in (1,1,30) do (
        timeout /t 2 /nobreak >nul
        curl -s -o nul --max-time 2 http://127.0.0.1:%FRONTEND_HOST_PORT%/ && goto :fe_ok
    )
    echo        [WARN] 前端未在 60 秒内就绪
    goto :fe_done
)
echo        已在运行
:fe_ok
:fe_done

echo.
echo ============================================================
echo   全部服务已就绪
echo   访问: http://localhost:%FRONTEND_HOST_PORT%    账号: admin / 123456
echo   后端: http://127.0.0.1:%SERVER_PORT%    向量: http://127.0.0.1:8701/health
echo ============================================================
endlocal
