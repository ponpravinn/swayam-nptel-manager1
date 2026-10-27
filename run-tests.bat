@echo off
title NPTEL Manager - Run Tests
set JAVA_HOME=C:\Program Files\Java\jdk-17
set MVN=C:\Users\Pon Prasad\.gemini\antigravity\scratch\maven\bin\mvn.cmd

echo.
echo  ============================================================
echo   SWAYAM NPTEL Assignment Manager - Test Suite
echo  ============================================================
echo.
echo  Running 18 integration tests...
echo.

"%MVN%" test

echo.
echo  ============================================================
echo  Done! Check results above.
echo  ============================================================
pause
