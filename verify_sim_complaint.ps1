$loginUrl = "http://localhost:8080/api/auth/login"
$simUrl = "http://localhost:8080/api/admin/bot-sim/interact"
$complaintUrl = "http://localhost:8080/api/admin/complaints"

$cred = @{ email = "admin@nagarparishad.in"; password = "password" }
$body = $cred | ConvertTo-Json

# 1. Login
try {
    Write-Host "Logging in..."
    $response = Invoke-RestMethod -Uri $loginUrl -Method Post -Body $body -ContentType "application/json"
    $token = $response.data.token
    $headers = @{ Authorization = "Bearer $token" }
}
catch {
    Write-Host "Login Failed: $_"
    exit
}

# Helper
function Send-Msg($msg, $mobile = "SIM_TESTER") {
    $payload = @{
        mobile  = $mobile
        message = $msg
    } | ConvertTo-Json
    try {
        $res = Invoke-RestMethod -Uri $simUrl -Method Post -Headers $headers -Body $payload -ContentType "application/json"
        Write-Host "User: $msg"
        Write-Host "Bot: $($res.response)"
        return $res.response
    }
    catch {
        Write-Host "Error: $_"
    }
}

# 2. Reset & Start
Write-Host "`n--- STARTING SIMULATION ---"
Send-Msg "RESET"
Send-Msg "Hi"

# 3. Flow
# Assuming flow: 
# 1. Lang (1=English)
# 2. Dept (1=First Dept?) 
# 3. Sub Issue?
# 4. Name
# 5. Desc
# 6. Photo (Skip)
# 7. Loc

Send-Msg "1" # English
Send-Msg "1" # Dept 1 (Sanitation?) -> "Garbage" etc?

# Need to check output to know what to send next. 
# But for script, I'll send likely inputs.
# Dept 1 usually "Sanitation". Options: "Garbage Issue", "Drainage Issue", "General Issue"
# Input is usually via Menu Number 1, 2, 3 or Text.
# If My Seeder worked:
# Sanitation -> "Garbage Issue" (1), "Drainage" (2) ...
# I'll send "1" again for Sub Issue.

Send-Msg "1" # Sub Issue 1
Send-Msg "John Doe" # Name
Send-Msg "Test Description via Script" # Desc
Send-Msg "Skip" # Photo
Send-Msg "Main Market" # Location

Write-Host "`n--- CHECKING COMPLAINT ---"
# Check latest complaint
try {
    $c = Invoke-RestMethod -Uri "$complaintUrl" -Method Get -Headers $headers
    $latest = $c.data | Sort-Object -Property id -Descending | Select-Object -First 1
    Write-Host "Latest Complaint ID: $($latest.complaintNo)"
    Write-Host "SubComplaintType: $($latest.subComplaintType)"
    
    if ($latest.complaintType -ne $null) {
        Write-Host "ComplaintType: $($latest.complaintType.nameEn)"
    }
    else {
        Write-Host "ComplaintType: NULL"
    }
}
catch {
    Write-Host "Check Failed: $_"
}
