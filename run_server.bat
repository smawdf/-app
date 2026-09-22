@echo off
chcp 65001 >nul
title 高糖小食 Go 后端服务 (:8085)
cd /d "D:\kaifa\myapp\orderdisk-server"

echo ===================================================
echo     高糖小食 (OrderDisk) 现代 Go 后端启动器
echo ===================================================
echo.
echo 当前电脑局域网 IP: 192.168.1.6
echo 服务监听端口: 8085
echo.
echo 手机端「服务器设置」请填写:
echo   主机 IP: 192.168.1.6
echo   端口: 8085
echo.
echo ---------------------------------------------------
echo 正在启动 orderdisk-server.exe ...
echo ===================================================
echo.

set PORT=8085
orderdisk-server.exe

pause
