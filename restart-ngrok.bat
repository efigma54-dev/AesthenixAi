@echo off
echo ========================================
echo  Restart ngrok + Update GitHub Webhook
echo ========================================
echo.

echo [1/3] Killing any existing ngrok process...
taskkill /IM ngrok.exe /F >nul 2>&1
timeout /t 2 >nul
echo Done.
echo.

echo [2/3] Verifying backend is running on port 8080...
netstat -ano | findstr :8080 >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Backend is NOT running on port 8080!
    echo Please start the backend first:
    echo   cd ai-code-reviewer
    echo   mvn spring-boot:run
    pause
    exit /b 1
)
echo Backend is running.
echo.

echo [3/3] Starting ngrok tunnel...
echo.
echo ============================================================
echo  IMPORTANT: When ngrok starts, copy the Forwarding URL
echo  (e.g. https://xxxx-xxxx-xxxx.ngrok-free.app)
echo.
echo  Then update your GitHub App webhook URL to:
echo  https://YOUR-NGROK-URL/api/webhooks/github
echo.
echo  GitHub App settings:
echo  https://github.com/settings/apps/aesthenixai
echo ============================================================
echo.

C:\ngrok.exe http 8082
