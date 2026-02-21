$loginParams = @{email = "admin@nagarparishad.in"; password = "password" }
try {
    $auth = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body ($loginParams | ConvertTo-Json) -ContentType "application/json" -ErrorAction Stop
    $token = $auth.data.accessToken
    Write-Host "Got Token"
}
catch {
    Write-Error "Login Failed: $_"
    exit 1
}

$url = "http://localhost:8080/api/public/whatsapp"
$headers = @{"Content-Type" = "application/x-www-form-urlencoded" }

function Create-Complaint {
    param($citizenName, $desc)
    # Simulate Chatbot Flow to create complaint
    # 1. Reset
    Invoke-WebRequest -Uri $url -Method Post -Body "From=whatsapp:+919999999999&To=whatsapp:+14155238886&Body=Hi" -Headers $headers -ErrorAction SilentlyContinue | Out-Null
    Start-Sleep -Milliseconds 2000
    # 2. Select Language (English)
    Invoke-WebRequest -Uri $url -Method Post -Body "From=whatsapp:+919999999999&To=whatsapp:+14155238886&Body=1" -Headers $headers -ErrorAction SilentlyContinue | Out-Null
    Start-Sleep -Milliseconds 2000
    # 3. Select Dept (1)
    Invoke-WebRequest -Uri $url -Method Post -Body "From=whatsapp:+919999999999&To=whatsapp:+14155238886&Body=1" -Headers $headers -ErrorAction SilentlyContinue | Out-Null
    Start-Sleep -Milliseconds 2000
    # 4. Name
    Invoke-WebRequest -Uri $url -Method Post -Body "From=whatsapp:+919999999999&To=whatsapp:+14155238886&Body=$citizenName" -Headers $headers -ErrorAction SilentlyContinue | Out-Null
    Start-Sleep -Milliseconds 2000
    # 5. Desc
    Invoke-WebRequest -Uri $url -Method Post -Body "From=whatsapp:+919999999999&To=whatsapp:+14155238886&Body=$desc" -Headers $headers -ErrorAction SilentlyContinue | Out-Null
    Start-Sleep -Milliseconds 2000
    # 6. Skip Photo
    Invoke-WebRequest -Uri $url -Method Post -Body "From=whatsapp:+919999999999&To=whatsapp:+14155238886&Body=Skip" -Headers $headers -ErrorAction SilentlyContinue | Out-Null
    Start-Sleep -Milliseconds 2000
    # 7. Location
    Invoke-WebRequest -Uri $url -Method Post -Body "From=whatsapp:+919999999999&To=whatsapp:+14155238886&Body=Test Location" -Headers $headers -ErrorAction SilentlyContinue | Out-Null
    
    Write-Host "Created Complaint for $citizenName"
}

Create-Complaint "Reject Tester" "This complaint is for testing rejection flow"
Start-Sleep -Seconds 2
Create-Complaint "Task Tester" "This complaint is for testing task creation"
Start-Sleep -Seconds 2
Create-Complaint "Task Flow Verify" "Specific verification for task creation logic"
