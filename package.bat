@echo off
echo ========================================
echo  LAN File Sharing — Package to JAR
echo ========================================
echo.

REM ── Build first ──
echo [1/4] Compiling sources...
if exist build rmdir /s /q build
mkdir build 2>nul

javac -d build -cp src src\common\SecurityUtil.java src\common\Protocol.java
if %errorlevel% neq 0 ( echo [ERROR] Compilation failed. & pause & exit /b 1 )

javac -d build -cp "build;src" src\server\Server.java src\server\ClientHandler.java
if %errorlevel% neq 0 ( echo [ERROR] Compilation failed. & pause & exit /b 1 )

javac -d build -cp "build;src" src\client\Client.java
if %errorlevel% neq 0 ( echo [ERROR] Compilation failed. & pause & exit /b 1 )
echo       Compiled OK

REM ── Create JAR output folder ──
if not exist dist mkdir dist

REM ── Create Server JAR manifest ──
echo [2/4] Packaging Server.jar...
echo Main-Class: server.Server> build\server-manifest.txt
echo.>> build\server-manifest.txt
jar cfm dist\FileShareServer.jar build\server-manifest.txt -C build common -C build server
if %errorlevel% neq 0 ( echo [ERROR] Server JAR failed. & pause & exit /b 1 )
echo       FileShareServer.jar created

REM ── Create Client JAR manifest ──
echo [3/4] Packaging Client.jar...
echo Main-Class: client.Client> build\client-manifest.txt
echo.>> build\client-manifest.txt
jar cfm dist\FileShareClient.jar build\client-manifest.txt -C build common -C build client
if %errorlevel% neq 0 ( echo [ERROR] Client JAR failed. & pause & exit /b 1 )
echo       FileShareClient.jar created

REM ── Copy run helpers ──
echo [4/4] Creating launchers...

(
echo @echo off
echo title LAN File Sharing - Server
echo java -jar "%~dp0FileShareServer.jar" %%*
echo if %%errorlevel%% neq 0 pause
) > dist\StartServer.bat

(
echo @echo off
echo title LAN File Sharing - Client
echo java -jar "%~dp0FileShareClient.jar"
echo if %%errorlevel%% neq 0 pause
) > dist\StartClient.bat

echo       Launchers created

echo.
echo ========================================
echo  PACKAGING COMPLETE
echo ========================================
echo.
echo  Output folder: dist\
echo.
echo  Files created:
echo    dist\FileShareServer.jar   — Double-click to start server
echo    dist\FileShareClient.jar   — Double-click to start client
echo    dist\StartServer.bat       — Alternative launcher (server)
echo    dist\StartClient.bat       — Alternative launcher (client)
echo.
echo  Copy the entire "dist" folder to any PC.
echo  Just double-click the .jar file to run!
echo.
pause
