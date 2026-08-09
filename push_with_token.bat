@echo off
setlocal enabledelayedexpansion

cd /d "C:\Users\KHAYAL VASAVA\.gemini\antigravity\scratch\UniDownloader"

:: Add Git and MobaXterm DLL runtime paths to environment PATH
set "PATH=C:\Program Files\Git\cmd;C:\Program Files\Git\bin;C:\Users\KHAYAL VASAVA\AppData\Roaming\MobaXterm\slash\mx86_64b\bin;C:\Users\KHAYAL VASAVA\AppData\Roaming\MobaXterm\slash\mx86_64b\usr\bin;%PATH%"

if exist "C:\Program Files\Git\cmd\git.exe" (
    set "GIT_CMD=C:\Program Files\Git\cmd\git.exe"
) else (
    set "GIT_CMD=C:\Users\KHAYAL VASAVA\AppData\Roaming\MobaXterm\slash\mx86_64b\bin\git.exe"
)

echo ===================================================
echo   UniDownloader - GitHub Instant Push Tool
echo ===================================================
echo.
set /p "GITHUB_TOKEN=Paste your GitHub Token (ghp_...): "

if "%GITHUB_TOKEN%"=="" (
    echo [ERROR] Token cannot be empty.
    pause
    exit /b
)

echo.
echo [1/3] Staging all files...
"%GIT_CMD%" add .

echo [2/3] Committing changes...
"%GIT_CMD%" commit -m "Update UniDownloader project files" >nul 2>&1

echo [3/3] Uploading to https://github.com/KHAYAL21VASAVA/UniDownloader.git...
"%GIT_CMD%" remote set-url origin https://%GITHUB_TOKEN%@github.com/KHAYAL21VASAVA/UniDownloader.git
"%GIT_CMD%" push -u origin main

echo.
echo ===================================================
echo   Upload complete! Check your repository:
echo   https://github.com/KHAYAL21VASAVA/UniDownloader
echo ===================================================
echo.
pause
