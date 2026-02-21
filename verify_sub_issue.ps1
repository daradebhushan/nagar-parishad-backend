$mobile = "9999999999"

function Simulate-Chat {
    param (
        [string]$text,
        [string]$MediaUrl = ""
    )
    $baseUrl = "http://localhost:8080/api/public/diag/simulate"
    $adminId = 1
    $mobile = "9999999999"
    
    Write-Host "User -> $text (Media: $MediaUrl)"
    $encodedText = [Uri]::EscapeDataString($text)
    $encodedMedia = [Uri]::EscapeDataString($MediaUrl)
    
    $finalUri = "$baseUrl?mobile=$mobile&text=$encodedText&adminId=$adminId"
    if ($MediaUrl -ne "") {
        $finalUri += "&mediaUrl=$encodedMedia"
    }
    # Write-Host "Requesting: $finalUri"
    try {
        # Use curl.exe for robustness against PS URI parsing quirks
        $response = & curl.exe -v "$finalUri" 2>&1
        Write-Host "Bot -> $response"
        return $response
    }
    catch {
        Write-Error "Error: $_"
        return $null
    }
    Start-Sleep -Seconds 0.2
}

# 1. Reset
Simulate-Chat "Hi"

# 2. Select Language (1=English)
Simulate-Chat "1"

# 3. Select Department (1=Health usually)
Simulate-Chat "1"

# 4. Select Sub-Issue (1=First Option)
# This stores "Issue: Something" into 'description' key in temp data
Simulate-Chat "1"

# 5. Name
Simulate-Chat "Test User"

# 6. Description Detail
# This stores into 'description_detail'
$detail = "Specific Details about the issue for verification"
Simulate-Chat $detail

# 7. Photo
# Pass a dummy URL (standard fake image)
$dummyPhoto = "https://via.placeholder.com/150"
Simulate-Chat "Photo" -MediaUrl $dummyPhoto

# 8. Location
$lastResponse = Simulate-Chat "Test Location"

# Extract Complaint No from response
# Response example: "Complaint Registered: CMP-1-1234"
if ($lastResponse -match "CMP-\d+-\d+") {
    $cmpNo = $matches[0]
    Write-Host "Complaint Created: $cmpNo"
    
    # Verify via Diagnostic API
    $diagUrl = "http://localhost:8080/api/public/diag/complaint/$cmpNo"
    try {
        $cmp = Invoke-RestMethod -Uri $diagUrl -Method Get
        Write-Host "--------------------------------------------------"
        Write-Host "VERIFICATION RESULTS:"
        Write-Host "Complaint No: $($cmp.complaintNo)"
        Write-Host "Sub Complaint Type: $($cmp.subComplaintType)"
        Write-Host "Description: $($cmp.description)"
        Write-Host "--------------------------------------------------"
        
        if ($cmp.subComplaintType -and $cmp.description -eq $detail) {
            Write-Host "SUCCESS: Sub-Issue is separated from Description."
        }
        else {
            Write-Error "FAILURE: Fields not separated correctly."
            Write-Host "Expected Description: '$detail'"
            Write-Host "Actual Description: '$($cmp.description)'"
        }
    }
    catch {
        Write-Error "Failed to fetch diagnostic info: $_"
    }
}
else {
    Write-Error "Could not parse Complaint No from response."
}
