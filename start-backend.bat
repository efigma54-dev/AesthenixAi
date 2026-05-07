@echo off
echo ========================================
echo AI Code Reviewer - Backend Startup
echo ========================================
echo.

echo [1/3] Checking port 8082...
netstat -ano | findstr :8082 >nul 2>&1
if %errorlevel% equ 0 (
    echo WARNING: Port 8082 is in use!
    echo.
    echo Finding process...
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8082 ^| findstr LISTENING') do (
        echo Killing PID: %%a
        taskkill /PID %%a /F >nul 2>&1
    )
    echo Port 8082 cleared.
    timeout /t 2 >nul
) else (
    echo Port 8082 is available.
)
echo.

echo [2/3] Checking Java...
java -version 2>&1 | findstr "version" >nul
if %errorlevel% neq 0 (
    echo ERROR: Java not found!
    echo Please install Java 17 or higher.
    pause
    exit /b 1
)
java -version
echo.

echo [3/3] Starting Spring Boot application...
echo.
echo Backend will be available at: http://localhost:8082
echo Press Ctrl+C to stop the server
echo.
echo ========================================
echo.

set OLLAMA_MODEL=qwen2.5-coder:7b
set OLLAMA_URL=http://localhost:11434
set SERVER_PORT=8082
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-DOLLAMA_MODEL=qwen2.5-coder:7b -DSERVER_PORT=8082"
