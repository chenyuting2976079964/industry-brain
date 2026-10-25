@echo off
chcp 6501 >nul
setlocal
rem ============================================================
rem  start-docker.bat - 企开得胜 中间件一键启动（幂等）
rem  已运行则保持不变, 缺则拉起, 数据卷不受影响
rem ============================================================
set "ROOT=%~dp0."
set "DOCKER="
if exist "C:\Program Files\Docker\Docker\resources\bin\docker.exe" set "DOCKER=C:\Program Files\Docker\Docker\resources\bin\docker.exe"
if exist "%LOCALAPPDATA%\Programs\DockerDesktop\resources\bin\docker.exe" set "DOCKER=%LOCALAPPDATA%\Programs\DockerDesktop\resources\bin\docker.exe"
if not defined DOCKER (where docker >nul 2>&1 && set "DOCKER=docker")
if not defined DOCKER (
  echo [X] 未找到 docker CLI, 请先安装 Docker Desktop
  pause & exit /b 1
)

echo ============================================
echo   企开得胜 - 中间件启动器 (docker compose)
echo ============================================
%DOCKER% info >nul 2>&1 || (
  echo [X] Docker Desktop 未运行, 请先启动它
  pause & exit /b 1
)

rem ---- 端口检查(仅提示) ----
call "%ROOT%check-env.bat"
echo.

rem ---- 拉起 ----
%DOCKER% compose -f "%ROOT%docker-compose.yml" up -d
if errorlevel 1 (
  echo [X] compose up 失败, 查看上方报错
  pause & exit /b 1
)

rem ---- 等待健康 (最多 90s) ----
set /a WAIT=0
:wait
%DOCKER% compose -f "%ROOT%docker-compose.yml" ps 2>nul | findstr /c:"(healthy)" >nul
set "OK=0"
for /f "tokens=2" %%h in ('%DOCKER% compose -f "%ROOT%docker-compose.yml" ps --format "{{.Status}}" 2^>nul') do (
  echo %%h | findstr /c:"(healthy)" >nul && set /a OK+=1
)
echo   健康容器 %OK%/5
if %OK% GEQ 5 goto :done
set /a WAIT+=1
if %WAIT% GEQ 18 (echo [!] 90s 内未全绿, 请用 docker ps 查看各容器状态; 数据卷安全, 可稍后重跑) & pause & exit /b 1
timeout /t 5 /nobreak >nul
goto :wait

:done
echo ============================================
echo   全部 5 个容器 healthy
echo ============================================
%DOCKER% ps --filter "name=industry_brain" --format "  {{.Names}}  {{.Status}}"
echo.
echo 宿主端口: MySQL 13307 | Redis 6379 | ES 9200 | Neo4j 7687/7474 | MinIO 9000/9001
pause
