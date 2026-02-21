# Test Concurrent Media Uploads via Simulation

$adminId = 1
$mobile = "SIM_CONCURRENT"
$url = "http://localhost:8080/api/public/bot-sim/interact"
$headers = @{ "Content-Type" = "application/json" }

function Sim($msg, $mediaUrl = $null) {
    $body = @{
        mobile   = $mobile
        message  = $msg
        adminId  = $adminId
        mediaUrl = $mediaUrl
        numMedia = if ($mediaUrl) { 1 } else { 0 }
    } | ConvertTo-Json
    
    try {
        $res = Invoke-RestMethod -Uri $url -Method Post -Body $body -ContentType "application/json" -ErrorAction Stop
        # Write-Host "Response: $($res.response)"
        return $res.response
    }
    catch {
        Write-Host "Error: $_"
        return "Error"
    }
}

# 1. Reset Flow
Write-Host "1. Resetting Flow..."
Sim "Hi" | Out-Null
Start-Sleep -Milliseconds 500

# 2. Select Language
Write-Host "2. English..."
Sim "1" | Out-Null
Start-Sleep -Milliseconds 500

# 3. Select Dept (Assume 1)
Write-Host "3. Dept 1..."
Sim "1" | Out-Null
Start-Sleep -Milliseconds 500

# 4. Name
Write-Host "4. Name..."
Sim "Concurrency Tester" | Out-Null
Start-Sleep -Milliseconds 500

# 5. Desc
Write-Host "5. Desc..."
Sim "Testing multi-threaded uploads" | Out-Null
Start-Sleep -Milliseconds 500

# 6. SEND 5 PHOTOS IN PARALLEL
Write-Host "6. Sending 5 Photos in Parallel..."

# We use a public image or one we can reach. 
# Using a placeholder URL that won't fail DNS (Google Logo or similar, or just a mock if internal logic allows)
# WhatsappService tries to download. 
# Let's use a non-existent URL? No, it will fail download and return original URL.
# That is FINE for filename generation testing! The filename is generated BEFORE download in my fix?
# Wait, let's check code.
# String fileName = "WA_..."
# then download.
# If download fails, it returns mediaUrl.
# IF it returns mediaUrl, then uniqueness is irrelevant for the file on disk (since none created).
# BUT, ComplaintService uses random suffix later.
# TO TEST WhatsappService uniqueness, we NEED successful download to disk.
# file:/// URI is supported by java.net.URL.

$testImagePath = "$PWD\seed_photo_v2.ps1" # Just using a text file as "image" to save bytes, it just copies stream.
$mediaUrl = "file:///$testImagePath".Replace("\", "/")

$scriptBlock = {
    param($url, $header, $body)
    Invoke-RestMethod -Uri $url -Method Post -Body $body -ContentType "application/json"
}

$jobs = @()
for ($i = 0; $i -lt 5; $i++) {
    $body = @{
        mobile   = "SIM_CONCURRENT" # Same Session!
        message  = "" # Empty for media?
        adminId  = 1
        mediaUrl = "file:///d:/anti gravity/backend/seed_photo_v2.ps1"
        numMedia = 1
    } | ConvertTo-Json
    
    $jobs += Start-Job -ScriptBlock $scriptBlock -ArgumentList $url, $headers, $body
}

Write-Host "Jobs started. Waiting..."
$jobs | Receive-Job -Wait | Out-Null

# 7. Next
Write-Host "7. Sending Next..."
$finalRes = Sim "Next"

# 8. Location
Write-Host "8. Location..."
$finalRes = Sim "Loc"

Write-Host "Final Response: $finalRes"
