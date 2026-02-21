
function Invoke-WhatsApp {
    param([string]$Body, [hashtable]$ExtraParams = @{})
    $params = @{
        From = "whatsapp:+918237930576"
        To   = "whatsapp:+14155238886"
        Body = $Body
    } 
    $ExtraParams.Keys | ForEach-Object { $params[$_] = $ExtraParams[$_] }
    
    try {
        Invoke-WebRequest -Uri "http://localhost:8080/api/public/whatsapp" -Method Post -Body $params -ErrorAction Stop
        Write-Host "Success: $Body"
    }
    catch {
        Write-Error "Failed: $Body - $_"
    }
}

Write-Host "1. Resetting..."
Invoke-WhatsApp -Body "Hi"
Start-Sleep -Seconds 1

Write-Host "2. Selecting Language (1)..."
Invoke-WhatsApp -Body "1"
Start-Sleep -Seconds 1

Write-Host "3. Selecting Dept (1)..."
Invoke-WhatsApp -Body "1"
Start-Sleep -Seconds 1

Write-Host "4. Selecting Issue (1)..."
Invoke-WhatsApp -Body "1"
Start-Sleep -Seconds 1

Write-Host "5. Sending Name..."
Invoke-WhatsApp -Body "Media Tester"
Start-Sleep -Seconds 1

Write-Host "6. Sending Description..."
Invoke-WhatsApp -Body "Testing Media Attachment"
Start-Sleep -Seconds 1

Write-Host "7. Sending Photo (MEDIA)..."
# Simulate Twilio Media Parameters
Invoke-WhatsApp -Body "" -ExtraParams @{ "NumMedia" = "1"; "MediaUrl0" = "http://example.com/test_image.jpg" }
Start-Sleep -Seconds 1

Write-Host "8. Sending Location..."
Invoke-WhatsApp -Body "Test Location"
Start-Sleep -Seconds 2

# Verify DB State
try {
    # Check latest complaint
    $check = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/complaints" -Method Get -Headers @{ "Authorization" = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiQURNSU4iLCJpZCI6NDgsInN1YiI6ImFoZXJjaGV0YW5hMkBnbWFpbC5jb20iLCJpYXQiOjE3Njg3MTQyNjQsImV4cCI6MTc2ODgwMDY2NH0.L9as7LzN22zZsfzTXgiJ77VJ0GThHpU6LWJk68jIG60" }
    $latest = $check | Sort-Object -Property id -Descending | Select-Object -First 1
    
    Write-Host "`n--- VERIFICATION RESULTS ---"
    Write-Host "Complaint ID: $($latest.id)"
    Write-Host "Photo URL: $($latest.photoUrl)"
    
    if ($latest.photoUrl -eq "http://example.com/test_image.jpg") {
        Write-Host "SUCCESS: Photo URL matches." -ForegroundColor Green
    }
    else {
        Write-Host "FAILURE: Photo URL mismatch. Got: $($latest.photoUrl)" -ForegroundColor Red
    }
    
    # Check related task (Assuming API doesn't expose it directly, we infer from logs or lack of error)
    # But wait, User wanted NO task.
    # The current Complaint entity has relatedTask field? Yes.
    # I should check if it's null.
    
    if ($latest.relatedTask -eq $null) {
        Write-Host "SUCCESS: No Related Task." -ForegroundColor Green
    }
    else {
        Write-Host "FAILURE: Task Created! ID: $($latest.relatedTask.id)" -ForegroundColor Red
    }

}
catch {
    Write-Host "Verification API Failed (Auth token might be expired or invalid?): $_"
}
