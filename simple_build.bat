@echo off
echo Cleaning build directories...
if exist build rmdir /s /q build
if exist dist rmdir /s /q dist
mkdir build
mkdir dist

echo Compiling...
javac -d build src/common/*.java
javac -d build -cp build;src src/server/*.java
javac -d build -cp build;src src/client/*.java

echo Packaging Server...
echo Main-Class: server.Server> build\server-manifest.txt
echo.>> build\server-manifest.txt
jar cfm dist\FileShareServer.jar build\server-manifest.txt -C build common -C build server

echo Packaging Client...
echo Main-Class: client.Client> build\client-manifest.txt
echo.>> build\client-manifest.txt
jar cfm dist\FileShareClient.jar build\client-manifest.txt -C build common -C build client

echo Creating Launchers...
(
echo @echo off
echo title LAN File Sharing - Server
echo java -jar "%%~dp0FileShareServer.jar" %%*
echo if %%errorlevel%% neq 0 pause
) > dist\StartServer.bat

(
echo @echo off
echo title LAN File Sharing - Client
echo java -jar "%%~dp0FileShareClient.jar"
echo if %%errorlevel%% neq 0 pause
) > dist\StartClient.bat

echo.
echo ==============================================
echo  BUILD COMPLETE - Artifacts in 'dist' folder
echo ==============================================
pause
