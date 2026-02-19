@echo off
setlocal
echo ========================================
echo  LAN File Sharing — Firewall Config
echo ========================================
echo.
echo This script will add Windows Firewall rules to allow
echo the LAN File Sharing System to work over the network.
echo.
echo [IMPORTANT] You must run this script as Administrator.
echo.
pause

echo.
echo Adding Firewall Rules...

:: 1. Allow TCP Port 5050 (File Transfer)
netsh advfirewall firewall add rule name="LAN File Sharing (TCP)" dir=in action=allow protocol=TCP localport=5050 profile=any
if %errorlevel% neq 0 (
    echo [ERROR] Failed to add TCP rule. Run as Admin!
    pause
    exit /b 1
)

:: 2. Allow UDP Port 8888 (Discovery)
netsh advfirewall firewall add rule name="LAN File Sharing (Discovery)" dir=in action=allow protocol=UDP localport=8888 profile=any
if %errorlevel% neq 0 (
    echo [ERROR] Failed to add UDP rule.
    pause
    exit /b 1
)

echo.
echo ========================================
echo  SUCCESS
echo ========================================
echo.
echo Firewall rules added successfully.
echo You may need to restart the application for changes to take effect.
echo.
pause
