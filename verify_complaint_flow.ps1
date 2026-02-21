# 1. Login to get Admin Token
$loginParams = @{email = "admin@nagarparishad.in"; password = "password" }
try {
    $auth = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body ($loginParams | ConvertTo-Json) -ContentType "application/json" -ErrorAction Stop
    $token = $auth.data.accessToken
    Write-Host "Got Token: $token"
}
catch {
    Write-Error "Login Failed: $_"
    exit 1
}

# 2. Simulate Chatbot Flow
$url = "http://localhost:8080/api/public/whatsapp"
$headers = @{"Content-Type" = "application/x-www-form-urlencoded" }

function Send-Msg {
    param($body)
    try {
        Invoke-WebRequest -Uri $url -Method Post -Body "From=whatsapp:+919876543210&To=whatsapp:+91Server&Body=$body" -Headers $headers -ErrorAction Stop | Out-Null
        Write-Host "Sent: $body"
    }
    catch {
        Write-Error "Failed to send $body : $_"
    }
}

Send-Msg "Hi"
Start-Sleep -Seconds 1
Send-Msg "1"
Start-Sleep -Seconds 1
Send-Msg "1"
Start-Sleep -Seconds 1
Send-Msg "John Chatbot"
Start-Sleep -Seconds 1
Send-Msg "Test Description via API"
Start-Sleep -Seconds 1
Send-Msg "Skip"
Start-Sleep -Seconds 1
Send-Msg "Pune Location"
Start-Sleep -Seconds 2

# 3. Verify Complaint Creation
try {
    $complaints = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/complaints" -Method Get -Headers @{Authorization = "Bearer $token" } -ErrorAction Stop
    $latest = $complaints | Select-Object -Last 1
    
    if ($latest) {
        Write-Host "Latest Complaint ID: $($latest.complaintNo)"
        Write-Host "Citizen Name: $($latest.citizenName)"
        Write-Host "Description: $($latest.description)"
        Write-Host "Status: $($latest.status)"
        
        if ($latest.citizenName -eq "John Chatbot" -and $latest.description -like "*Test Description via API*") {
            Write-Host "SUCCESS: Complaint Verified!" -ForegroundColor Green
        }
        else {
            Write-Host "FAILURE: Data mismatch." -ForegroundColor Red
        }
    }
    else {
        Write-Host "FAILURE: No complaints found." -ForegroundColor Red
    }
}
catch {
    Write-Error "Failed to fetch complaints: $_"
}
