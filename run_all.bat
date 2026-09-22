@echo off
chcp 65001 >nul
title 高糖小食 · 后端 + 公网隧道 一键启动
cd /d "D:\kaifa\myapp\orderdisk-server"

echo ==========================================================
echo    高糖小食 (OrderDisk) 后端服务 + 公网隧道 一键启动
echo ==========================================================
echo.
echo  【手机端要填的服务器地址】(二选一，App 会自动探测)
echo.
echo    1) 公网地址（手机在外网 / 4G 也能连，推荐）
echo       https://orderdisk-couple.loca.lt
echo.
echo    2) 局域网地址（在家同一个 WiFi 时最快）
echo       192.168.1.6   端口 8085
echo.
echo  注意：本窗口不要关闭，关掉服务就断了。
echo ==========================================================
echo.

if not exist "orderdisk-server.exe" (
  echo [错误] 找不到 orderdisk-server.exe，请先执行: go build -o orderdisk-server.exe ./cmd/server
  pause
  exit /b 1
)

echo [1/2] 启动 Go 后端服务 (端口 8085)...
start "OrderDisk-Backend" cmd /k "set PORT=8085 && orderdisk-server.exe"

echo [2/2] 启动公网隧道 (orderdisk-couple.loca.lt)...
timeout /t 3 /nobreak >nul
start "OrderDisk-Tunnel" cmd /k "npx -y localtunnel --port 8085 --subdomain orderdisk-couple"

echo.
echo 启动完成！两个新窗口分别跑着 后端服务 和 公网隧道。
echo 手机打开「高糖小食·新版」，直接登录即可（无需手动配地址）。
echo.
echo 若手机连不上，请确认：
echo   1) 两个黑窗口都还在运行
echo   2) 手机有网络（公网隧道模式）
echo   3) 家用 WiFi 模式下：控制面板 - Windows Defender 防火墙 -
echo      "允许应用通过防火墙" 中放行 orderdisk-server.exe
echo.
pause
