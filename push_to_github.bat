@echo off
cd /d "C:\Users\KHAYAL VASAVA\.gemini\antigravity\scratch\UniDownloader"
set GIT_EXE="C:\Users\KHAYAL VASAVA\AppData\Roaming\MobaXterm\slash\mx86_64b\bin\git.exe"

echo ===================================================
echo [UniDownloader] Pushing code to GitHub...
echo Repository: https://github.com/KHAYAL21VASAVA/UniDownloader.git
echo ===================================================
echo.

%GIT_EXE% add .
%GIT_EXE% commit -m "Update UniDownloader project files" >nul 2>&1
%GIT_EXE% push -u origin main

echo.
pause
