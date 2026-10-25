@echo off
chcp 6501 >nul
setlocal EnableDelayedExpansion
rem ============================================================
rem  check-env.bat - 检查 .env 中宿主端口占用情况（仅提示）
rem ============================================================
set "ENVFILE=%~dp0.env"
echo ============================================
echo   企开得胜 - 宿主端口占用检查
echo ============================================
set "CHECKED=0"
if not exist "%ENVFILE%" (
  echo [提示] 未找到 .env, 使用默认端口
  for %%p in (13307 6379 9200 9300 7687 7474 9000 9001) do call :probe "默认" %%p
  goto :eof
)
for /f "tokens=1,* delims==" %%a in ('findstr /b "MYSQL_HOST_PORT REDIS_HOST_PORT ES_HOST_PORT ES_TRANSPORT_PORT NEO4J_BOLT_PORT NEO4J_HTTP_PORT MINIO_API_PORT MINIO_CONSOLE_PORT" "%ENVFILE%"') do (
  set "V=%%b"
  for /f "tokens=1 delims= " %%p in ("%V%") do (
    set /a CHECKED+=1
    call :probe "%%a" "%%p"
  )
)
if "%CHECKED%"=="0" echo [提示] .env 未定义宿主端口, 将用默认值
echo.
echo 说明: [OK]=可绑定  [!]=已被占用(改 .env 对应 *_HOST_PORT 后重跑)
exit /b 0

:probe
netstat -ano | findstr /r /c:":%2 " | findstr /i "LISTEN" >nul 2>&1
if errorlevel 1 (echo   [%1:%2] [OK] 空闲) else (echo   [%1:%2] [!] 被占用)
exit /b 0
