# Quick Webhook Test Script
# Tests if GitHub can reach your backend through ngrok

Write-Host "GitHub Webhook Quick Test" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""

# Test 1: Check ngrok URL
$ngrokUrl = "https://handprint-headlamp-conceal.ngrok-free.dev"
Write-Host "Test 1: Testing ngrok connectivity to $ngrokUrl" -ForegroundColor Yellow

try {
    $response = Invoke-WebRequest "$ngrokUrl/api/github/webhook" `
        -Method OPTIONS `
        -SkipCertificateCheck `
        -TimeoutSec 5 `
        -ErrorAction Stop
    
    Write-Host "SUCCESS: ngrok URL is accessible!" -ForegroundColor Green
    Write-Host "   Status: $($response.StatusCode)" -ForegroundColor Green
    Write-Host ""
} catch {
    Write-Host "FAILED: Cannot reach ngrok URL" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "   Check these things:" -ForegroundColor Red
    Write-Host "   1. Is ngrok running? (ngrok http 8080)" -ForegroundColor Red
    Write-Host "   2. Is the URL correct? (check ngrok terminal)" -ForegroundColor Red
    Write-Host "   3. Is there internet connection?" -ForegroundColor Red
    Write-Host ""
}

# Test 2: Check if backend would respond
Write-Host "Test 2: Checking if backend is running on localhost:8080" -ForegroundColor Yellow

try {
    $response = Invoke-WebRequest "http://localhost:8080/actuator/health" `
        -SkipCertificateCheck `
        -TimeoutSec 3 `
        -ErrorAction Stop
    
    $health = $response.Content | ConvertFrom-Json
    Write-Host "SUCCESS: Backend is RUNNING!" -ForegroundColor Green
    Write-Host "   Status: $($health.status)" -ForegroundColor Green
    Write-Host ""
} catch {
    Write-Host "INFO: Backend is NOT running yet" -ForegroundColor Yellow
    Write-Host "   This is expected. You can still test webhook with:" -ForegroundColor Yellow
    Write-Host "   - Docker mock server (see WEBHOOK_TESTING_GUIDE.md)" -ForegroundColor Yellow
    Write-Host "   - Create real PR in GitHub to trigger ngrok" -ForegroundColor Yellow
    Write-Host ""
}

# Test 3: Show what to do next
Write-Host "Next Steps:" -ForegroundColor Cyan
Write-Host "1. Go to your GitHub repository" -ForegroundColor Cyan
Write-Host "2. Create or update a Pull Request" -ForegroundColor Cyan
Write-Host "3. GitHub will send a webhook to your ngrok URL" -ForegroundColor Cyan
Write-Host "4. Watch the ngrok panel: http://127.0.0.1:4040" -ForegroundColor Cyan
Write-Host "5. Check GitHub Settings -> Webhooks -> Recent Deliveries" -ForegroundColor Cyan
Write-Host ""
Write-Host "SUCCESS = Status 200 in Recent Deliveries" -ForegroundColor Green
Write-Host ""
