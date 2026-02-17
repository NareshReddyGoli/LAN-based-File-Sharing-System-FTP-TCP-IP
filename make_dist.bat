@echo off
setlocal
echo ========================================
echo  Make Distribution (dist)
echo ========================================
echo.

REM 1. Clean and Build
echo [1/3] Cleaning and Compiling...
if exist build rmdir /s /q build
if exist dist rmdir /s /q dist
mkdir build
mkdir dist

javac -d build src\common\*.java
javac -d build -cp build;src src\server\*.java
javac -d build -cp build;src src\client\*.java

if %errorlevel% neq 0 (
    echo [ERROR] Compilation failed.
    pause
    exit /b 1
)

REM 2. Create JARs in dist
echo.
echo [2/3] Creating JARs...

REM Server
echo Main-Class: server.Server> build\server-manifest.txt
echo.>> build\server-manifest.txt
jar cfm dist\FileShareServer.jar build\server-manifest.txt -C build common -C build server
if %errorlevel% neq 0 exit /b 1

REM Client
echo Main-Class: client.Client> build\client-manifest.txt
echo.>> build\client-manifest.txt
jar cfm dist\FileShareClient.jar build\client-manifest.txt -C build common -C build client
if %errorlevel% neq 0 exit /b 1

REM 3. Create Batch Files in dist
echo.
echo [3/3] Creating Launcher Scripts...

REM StartServer.bat
(
echo @echo off
echo title LAN File Sharing - Server
echo echo Starting Server...
echo echo.
echo java -jar "%%~dp0FileShareServer.jar"
echo if %%errorlevel%% neq 0 pause
) > dist\StartServer.bat

REM StartClient.bat
(
echo @echo off
echo title LAN File Sharing - Client
echo echo Starting Client...
echo echo.
echo java -jar "%%~dp0FileShareClient.jar"
echo if %%errorlevel%% neq 0 pause
) > dist\StartClient.bat

echo.
echo ========================================
echo  DISTRIBUTION CREATED in 'dist' folder
echo ========================================
echo.
pause
