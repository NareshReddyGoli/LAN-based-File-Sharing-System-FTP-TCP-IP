@echo off
echo ========================================
echo  LAN File Sharing — Client
echo ========================================
echo.

REM ── Auto-build if needed ──
if not exist "build\client" (
    echo Build not found — compiling...
    call build.bat
    if %errorlevel% neq 0 (
        echo [ERROR] Build failed. Cannot start client.
        pause
        exit /b 1
    )
)

echo Starting client GUI...
echo.

java -cp "build" client.Client

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Client exited with code %errorlevel%
)
pause
