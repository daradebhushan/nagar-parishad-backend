$baseUrl = "http://localhost:8080/api"

function Invoke-PostRequest($url, $body) {
    try {
        $response = Invoke-RestMethod -Uri $url -Method Post -Body ($body | ConvertTo-Json) -ContentType "application/json" -ErrorAction Stop
        return $response
    }
    catch {
        Write-Host "Error calling $url : $_" -ForegroundColor Red
        return $null
    }
}

function Invoke-GetRequest($url) {
    try {
        $response = Invoke-RestMethod -Uri $url -Method Get -ErrorAction Stop
        return $response
    }
    catch {
        Write-Host "Error calling $url : $_" -ForegroundColor Red
        return $null
    }
}

Write-Host "1. Seeding Twilio Config..." -ForegroundColor Cyan
$seedTwilio = Invoke-GetRequest "$baseUrl/public/diag/seed-twilio"
Write-Host "Response: $seedTwilio"

Write-Host "`n2. Seeding Chatbot Config..." -ForegroundColor Cyan
$seedConfig = Invoke-GetRequest "$baseUrl/public/diag/seed-config"
Write-Host "Response: $seedConfig"

Write-Host "`n3. Activating Departments..." -ForegroundColor Cyan
Invoke-GetRequest "$baseUrl/public/diag/cleanup-departments" | Out-Null
Write-Host "Departments activated."

Write-Host "`n4. Simulating Chat (Admin 1)..." -ForegroundColor Cyan
$payload = @{
    mobile  = "9999999999"
    message = "Hi"
    adminId = "1"
}
$response = Invoke-PostRequest "$baseUrl/public/bot-sim/interact" $payload
Write-Host "Bot Response: $($response.response)"

if ($response.response -match "Welcome to Nagar Panchayat" -or $response.response -match "Welcome to") {
    Write-Host "SUCCESS: Received Admin 1 Welcome Message" -ForegroundColor Green
}
elseif ($response.response -match "Select your preferred language" -or $response.response -match "Language") {
    Write-Host "SUCCESS: Received Language Selection" -ForegroundColor Green
    
    # Select Language
    $payload.message = "1"
    $response = Invoke-PostRequest "$baseUrl/public/bot-sim/interact" $payload
    Write-Host "Bot Response (Lang Select): $($response.response)"
    
    if ($response.response -match "Welcome to Nagar Panchayat" -or $response.response -match "Welcome to") {
        Write-Host "SUCCESS: Flow proceeded to Welcome" -ForegroundColor Green
    }
    else {
        Write-Host "FAILED: Did not get Welcome message" -ForegroundColor Red
    }
}
else {
    Write-Host "FAILED: Unexpected response" -ForegroundColor Red
}

Write-Host "`n5. Testing Invalid Admin (Admin 999)..." -ForegroundColor Cyan
$payload.adminId = "999"
$payload.message = "Hi"
try {
    $response = Invoke-PostRequest "$baseUrl/admin/bot-sim/interact" $payload
    Write-Host "Bot Response: $($response.response)"
}
catch {
    Write-Host "Caught expected error for missing admin (or handled gracefully)" -ForegroundColor Yellow
}
