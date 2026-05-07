@echo off
echo ========================================
echo AI Code Reviewer - Frontend Startup
echo ========================================
echo.

echo [1/2] Checking Node.js...
node -v >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Node.js not found!
    echo Please install Node.js from https://nodejs.org
    pause
    exit /b 1
)
node -v
npm -v
echo.

echo [2/2] Starting Vite dev server...
cd frontend-react
echo.
echo Frontend will be available at: http://localhost:5173
echo Press Ctrl+C to stop the server
echo.
echo ========================================
echo.

npm run dev
