@echo off
echo ========================================
echo  LAN File Sharing — Server
echo ========================================
echo.

REM ── Auto-build if needed ──
if not exist "build\server" (
    echo Build not found — compiling...
    call build.bat
    if %errorlevel% neq 0 (
        echo [ERROR] Build failed. Cannot start server.
        pause
        exit /b 1
    )
)

REM ── Parse faculty parameter ──
set FACULTY=faculty1
if not "%~1"=="" set FACULTY=%~1

echo Starting server for: %FACULTY%
echo Press Ctrl+C to stop.
echo.

java -cp "build" server.Server %FACULTY%

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Server exited with code %errorlevel%
)
pause
