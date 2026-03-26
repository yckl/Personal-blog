@echo off
setlocal EnableExtensions EnableDelayedExpansion
chcp 65001 >nul

cd /d "%~dp0"

echo ========================================
echo   Personal Blog - Dev Starter
echo ========================================
echo.

call :resolve_maven
if errorlevel 1 (
  pause
  exit /b 1
)

where npm >nul 2>nul
if errorlevel 1 (
  echo [ERROR] npm not found in PATH.
  pause
  exit /b 1
)

echo [0/3] Clearing occupied ports...
call :kill_port 8088
call :kill_port 5173
call :kill_port 5174

echo.
echo Maven: %MAVEN_CMD%
echo.

echo [1/3] Starting backend on http://127.0.0.1:8088
start "blog-server" /D "%~dp0blog-server" cmd /k "call \"%MAVEN_CMD%\" spring-boot:run"

timeout /t 2 >nul

echo [2/3] Starting blog web on http://127.0.0.1:5173
start "blog-web" /D "%~dp0blog-web" cmd /k "npm run dev"

timeout /t 1 >nul

echo [3/3] Starting admin on http://127.0.0.1:5174
start "blog-admin" /D "%~dp0blog-admin" cmd /k "npm run dev"

echo.
echo Done.
echo - Frontend: http://127.0.0.1:5173
echo - Admin:    http://127.0.0.1:5174
echo - Backend:  http://127.0.0.1:8088
echo.
echo Close the spawned terminal windows to stop the services.
echo.
pause
exit /b 0

:resolve_maven
set "MAVEN_CMD="
for /f "delims=" %%i in ('where mvn.cmd 2^>nul') do (
  if not defined MAVEN_CMD set "MAVEN_CMD=%%i"
)
if not defined MAVEN_CMD if exist "D:\maven\bin\mvn.cmd" set "MAVEN_CMD=D:\maven\bin\mvn.cmd"
if not defined MAVEN_CMD if exist "%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.6-bin\3311e1d4\apache-maven-3.9.6\bin\mvn.cmd" set "MAVEN_CMD=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.6-bin\3311e1d4\apache-maven-3.9.6\bin\mvn.cmd"

if not defined MAVEN_CMD (
  echo [ERROR] Maven not found.
  echo Checked: PATH, D:\maven\bin\mvn.cmd, Maven wrapper cache.
  exit /b 1
)
exit /b 0

:kill_port
set "PORT=%~1"
set "FOUND_PID="
for /f "usebackq delims=" %%p in (`powershell -NoProfile -Command "$ErrorActionPreference='SilentlyContinue'; Get-NetTCPConnection -LocalPort %PORT% -State Listen | Select-Object -ExpandProperty OwningProcess -Unique"`) do (
  if not "%%p"=="" (
    echo   - Closing PID %%p on port %PORT%
    taskkill /PID %%p /F >nul 2>nul
    set "FOUND_PID=1"
  )
)
if not defined FOUND_PID echo   - Port %PORT% is free
exit /b 0
