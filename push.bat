@echo off
title Push SWAYAM NPTEL Manager to GitHub
echo ========================================================
echo  Pushing SWAYAM NPTEL Assignment Manager to GitHub...
echo  Target: https://github.com/ponpravinn/swayam-nptel-manager1.git
echo ========================================================
echo.
"C:\Users\Pon Prasad\.gemini\antigravity\scratch\git\cmd\git.exe" push -u origin main
echo.
if %errorlevel% equ 0 (
    echo ========================================================
    echo  SUCCESS! Your project has been pushed to GitHub!
    echo ========================================================
) else (
    echo ========================================================
    echo  Push failed.
    echo ========================================================
)
echo.
pause
