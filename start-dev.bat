@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul

cd /d "%~dp0"

echo ========================================
echo   Personal Blog - Dev Starter
echo ========================================
echo.

set "MAVEN_CMD=mvn"
where mvn >nul 2>nul
if errorlevel 1 (
  set "MAVEN_CMD=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.6-bin\3311e1d4\apache-maven-3.9.6\bin\mvn.cmd"
)

if not exist "%MAVEN_CMD%" (
  echo [ERROR] Maven not found.
  echo Please install Maven or update start-dev.bat with the correct mvn.cmd path.
  echo Current expected path:
  echo   %MAVEN_CMD%
  pause
  exit /b 1
)

where npm >nul 2>nul
if errorlevel 1 (
  echo [ERROR] npm not found in PATH.
  pause
  exit /b 1
)

echo [1/3] Starting backend on http://127.0.0.1:8088
start "blog-server" cmd /k "cd /d "%~dp0blog-server" && "%MAVEN_CMD%" spring-boot:run"

timeout /t 2 >nul

echo [2/3] Starting blog web on http://127.0.0.1:5173
start "blog-web" cmd /k "cd /d "%~dp0blog-web" && npm run dev"

timeout /t 1 >nul

echo [3/3] Starting admin on http://127.0.0.1:5174
start "blog-admin" cmd /k "cd /d "%~dp0blog-admin" && npm run dev"

echo.
echo Done.
echo - Frontend: http://127.0.0.1:5173
echo - Admin:    http://127.0.0.1:5174
echo - Backend:  http://127.0.0.1:8088
echo.
echo Close the spawned terminal windows to stop the services.
echo.
pause
