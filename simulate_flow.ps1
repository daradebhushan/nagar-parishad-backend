
$baseUrl = "http://localhost:8080/api/public/diag/simulate"
$mobile = "999"
$adminId = "1"

# Fetch Data
$data = Invoke-RestMethod -Uri "http://localhost:8080/api/public/diag/data" -Method Get
$deptIndex = 0
$targetIndex = 0
$data | ForEach-Object {
    if ($_.active) {
        $deptIndex++
        if ($_.id -eq 6) { $targetIndex = $deptIndex } # Use ID 6 for Sanitation
    }
}
if ($targetIndex -eq 0) { $targetIndex = 1 }

$lastResponse = ""
function Call-Sim {
    param($msg, $media)
    $url = $script:baseUrl + "?mobile=$script:mobile&adminId=$script:adminId"
    if ($msg) { $url += "&message=" + [Uri]::EscapeDataString($msg) }
    else { $url += "&message=" }
    if ($media) { $url += "&mediaUrl=" + [Uri]::EscapeDataString($media) }
    
    try {
        $res = Invoke-RestMethod -Uri $url -Method Get
        Write-Host "Response: $res"
        $script:lastResponse = $res
    }
    catch {
        Write-Host "Error calling $url : $_"
    }
    Start-Sleep -Milliseconds 1000
}

Write-Host "1. Reset"
Call-Sim -msg "RESET"

Write-Host "2. Select Dept ($targetIndex)"
Call-Sim -msg "$targetIndex"

Write-Host "3. Select SubIssue (1)"
Call-Sim -msg "1"

Write-Host "4. Enter Name"
Call-Sim -msg "SimUser"

Write-Host "5. Enter Details"
Call-Sim -msg "SimDetail"

Write-Host "6. Photo 1"
Call-Sim -msg "" -media "https://placehold.co/600x400/png"

Write-Host "7. Photo 2"
Call-Sim -msg "" -media "https://placehold.co/400x400/png"

Write-Host "8. Next"
Call-Sim -msg "Next"

Write-Host "9. Location (Submit)"
Call-Sim -msg "SimLoc"

# Extract ID from Response
# Response: ... तक्रार क्रमांक: CMP-6-1234 ...
if ($lastResponse -match "CMP-[\d-]+") {
    $createdId = $matches[0]
    Write-Host "Created ID from Bot: $createdId"
}
else {
    Write-Host "Could not find ID in response."
}

Write-Host "10. Checking Complaints"
$complaints = Invoke-RestMethod -Uri "http://localhost:8080/api/public/diag/complaints" -Method Get
$last = $complaints | Select-Object -Last 1
Write-Host "Last Complaint in DB: $($last.complaintNo)"

if ($createdId -eq $last.complaintNo) {
    Write-Host "MATCH! Complaint Persisted."
    Write-Host "Attachments: $($last.attachments.Count)"
}
else {
    Write-Host "MISMATCH! Complaint Rolled Back?"
}
