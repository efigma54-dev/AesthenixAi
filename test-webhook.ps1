#!/usr/bin/env pwsh
# Quick Webhook Test Script
# Tests if GitHub can reach your backend through ngrok

Write-Host "🔌 GitHub Webhook Quick Test" -ForegroundColor Cyan
Write-Host "================================`n"

# Test 1: Check ngrok URL
$ngrokUrl = "https://handprint-headlamp-conceal.ngrok-free.dev"
Write-Host "Test 1️⃣ : Testing ngrok connectivity to $ngrokUrl" -ForegroundColor Yellow

try {
    $response = Invoke-WebRequest "$ngrokUrl/api/github/webhook" `
        -Method OPTIONS `
        -SkipCertificateCheck `
        -TimeoutSec 5 `
        -ErrorAction Stop
    
    Write-Host "✅ ngrok URL is accessible!" -ForegroundColor Green
    Write-Host "   Status: $($response.StatusCode)`n"
} catch {
    Write-Host "❌ Cannot reach ngrok URL" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)"
    Write-Host "   → Check these things:"
    Write-Host "   1. Is ngrok running? (ngrok http 8080)"
    Write-Host "   2. Is the URL correct? (check ngrok terminal)"
    Write-Host "   3. Is there internet connection?`n"
}

# Test 2: Check if backend would respond
Write-Host "Test 2️⃣ : Checking if backend is running on localhost:8080" -ForegroundColor Yellow

try {
    $response = Invoke-WebRequest "http://localhost:8080/actuator/health" `
        -SkipCertificateCheck `
        -TimeoutSec 3 `
        -ErrorAction Stop
    
    $health = $response.Content | ConvertFrom-Json
    Write-Host "✅ Backend is RUNNING!" -ForegroundColor Green
    Write-Host "   Status: $($health.status)`n"
} catch {
    Write-Host "⚠️  Backend is NOT running (expected for now)" -ForegroundColor Yellow
    Write-Host "   This is OK - you can still test with:"
    Write-Host "   • Docker mock server (see WEBHOOK_TESTING_GUIDE.md)"
    Write-Host "   • Direct POST test (see guide)`n"
}

# Test 3: Show what to do next
Write-Host "📋 Next Steps:" -ForegroundColor Cyan
Write-Host "1. Go to your GitHub repository"
Write-Host "2. Create or update a Pull Request"
Write-Host "3. GitHub will send a webhook to your ngrok URL"
Write-Host "4. Watch the ngrok panel: http://127.0.0.1:4040"
Write-Host "5. Check GitHub Settings → Webhooks → Recent Deliveries"
Write-Host "`n✅ If you see Status 200 → Webhook is working!`n"

# Test 4: Create payload test (optional)
Write-Host "🧪 (Optional) Send test webhook payload?" -ForegroundColor Yellow
$testPayload = @{
    "action" = "opened"
    "pull_request" = @{
        "number" = 1
        "title" = "Test PR"
    }
} | ConvertTo-Json

Write-Host "Send test to $ngrokUrl/api/github/webhook"
Write-Host "`n"
