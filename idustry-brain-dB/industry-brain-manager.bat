@echo off
chcp 6501 >nul
echo ============================================
echo  [已迁移] 本目录旧编排已停用
echo ============================================
echo  中间件统一由项目根目录管理:
echo    docker-compose.yml + .env + start-docker.bat
echo
echo  注意: 本目录 compose 使用旧默认端口(3307)与新建命名卷,
echo  与运行中的数据卷(32.9万企业)不匹配, 在此执行 up 会产生
echo  一套独立的第二实例, 请勿使用。
echo
echo  请运行: %~dp0..\start-docker.bat
pause
