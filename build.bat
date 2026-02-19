@echo off
echo ========================================
echo  LAN File Sharing System — Build
echo ========================================
echo.

REM ── Check Java ──
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java is not installed or not in PATH.
    echo         Install JDK 8+ from https://adoptium.net/
    pause
    exit /b 1
)
javac -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java compiler (javac) not found.
    echo         Make sure you have JDK installed, not just JRE.
    pause
    exit /b 1
)

REM ── Clean previous build ──
if exist "build" (
    echo Cleaning previous build...
    rmdir /s /q build
)

REM ── Create output directories ──
mkdir build 2>nul
mkdir build\common 2>nul
mkdir build\server 2>nul
mkdir build\client 2>nul

REM ── Compile common module first (no dependencies) ──
echo [1/3] Compiling common module...
javac -d build -cp src src\common\SecurityUtil.java src\common\Protocol.java
if errorlevel 1 (
    echo [ERROR] Common module compilation failed.
    pause
    exit /b 1
)
echo       OK

REM ── Compile server (depends on common) ──
echo [2/3] Compiling server module...
javac -d build -cp "build;src" src\server\Server.java src\server\ClientHandler.java
if errorlevel 1 (
    echo [ERROR] Server module compilation failed.
    pause
    exit /b 1
)
echo       OK

REM ── Compile client (depends on common) ──
echo [3/3] Compiling client module...
javac -d build -cp "build;src" src\client\Client.java
if errorlevel 1 (
    echo [ERROR] Client module compilation failed.
    pause
    exit /b 1
)
echo       OK

echo.
echo ========================================
echo  BUILD SUCCESSFUL
echo ========================================
echo.
echo  Run the server:
echo    run_server.bat [faculty1^|faculty2]
echo.
echo  Run the client:
echo    run_client.bat
echo.
pause
