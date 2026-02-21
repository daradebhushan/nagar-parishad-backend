$loginUrl = "http://localhost:8080/api/auth/login"
$seedUrl = "http://localhost:8080/api/admin/seeder/seed-sub-questions"
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
    
    # 2. Seed
    Write-Host "Seeding Sub-Questions..."
    $seedRes = Invoke-RestMethod -Uri $seedUrl -Method Post -Headers $headers
    Write-Host "Seed Result: $seedRes"

}
catch {
    Write-Host "Setup Failed: $_"
    exit
}

# Helper
function Send-Msg($msg) {
    $payload = @{
        mobile  = "SIM_MARATHI_TESTER_3"
        message = $msg
    } | ConvertTo-Json
    try {
        $res = Invoke-RestMethod -Uri $simUrl -Method Post -Headers $headers -Body $payload -ContentType "application/json"
        # Write-Host "Bot: $($res.response)"
        return $res.response
    }
    catch {
        Write-Host "Error: $_"
    }
}

# 3. Simulate Flow
Write-Host "`n--- STARTING SIMULATION ---"
Send-Msg "RESET"
Send-Msg "Hi"
Send-Msg "1" # English (Bot will show departments)

# I need to pick Electricity Dept. I don't know the ID.
# I will try to parse the previous response or just guess.
# Usually 1=Water, 2=Road, 3=Health, 4=Electricity? 
# I will try sending "4". If it fails, I'll try finding "Electricity" or "Street Light" in response text if I could interactive, 
# but here I just send "4" assuming the seeder updated it.
# Actually, the user's list has 1, 2, 3 as sub-issues.
# Let's assume Dept 4 is good.

$deptRes = Send-Msg "4" # Select Dept 4 (Hope it's Electric)

# Now select Sub Issue "1" (Should be "विद्युत पोलवरील दिवा बंद")
Send-Msg "1" 

Send-Msg "Marathi User" # Name
Send-Msg "Sub Issue Test" # Desc
Send-Msg "Skip" # Photo
Send-Msg "Pune" # Location

Write-Host "`n--- CHECKING COMPLAINT ---"
try {
    $c = Invoke-RestMethod -Uri "$complaintUrl" -Method Get -Headers $headers
    $latest = $c.data | Sort-Object -Property id -Descending | Select-Object -First 1
    Write-Host "Latest Complaint ID: $($latest.complaintNo)"
    Write-Host "SubComplaintType: $($latest.subComplaintType)"
    
    if ($latest.subComplaintType) {
        $bytes = [System.Text.Encoding]::UTF8.GetBytes($latest.subComplaintType)
        $hex = [System.BitConverter]::ToString($bytes)
        Write-Host "Hex: $hex"
    }
    else {
        Write-Host "SubComplaintType is NULL"
    }
}
catch {
    Write-Host "Check Failed: $_"
}
