@echo off
title SWAYAM NPTEL Assignment Manager
set JAVA_HOME=C:\Program Files\Java\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%

echo.
echo  ============================================================
echo   SWAYAM NPTEL Assignment Manager
echo  ============================================================
echo.
echo  Starting server... please wait 5-10 seconds...
echo.
echo  Once started, open your Chrome browser and visit:
echo    http://localhost:8080
echo.
echo  Demo Login Credentials:
echo    Student : student@demo.com       / Student@123
echo    Admin   : admin@nptelmanager.com / Admin@123
echo.
echo  Press Ctrl+C to stop the server
echo  ============================================================
echo.

if exist "target\nptel-manager-1.0.0.jar" (
    java -jar "target\nptel-manager-1.0.0.jar"
) else (
    "C:\Users\Pon Prasad\.gemini\antigravity\scratch\maven\bin\mvn.cmd" spring-boot:run
)

pause
