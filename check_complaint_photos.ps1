$adminEmail = "bhushandarade1407@gmail.com"
$adminPassword = "admin123"
$baseUrl = "http://localhost:8080/api"

$loginBody = @{
    email    = $adminEmail
    password = $adminPassword
} | ConvertTo-Json -Depth 5

Write-Host "Sending Login Body: $loginBody"

try {
    $loginRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $loginBody -ContentType "application/json" -ErrorAction Stop
    $token = $loginRes.data.accessToken
    Write-Host "Login Successful. Token Length: $($token.Length)"
    Write-Host "Token Start: $($token.Substring(0, 10))..."
}
catch {
    Write-Host "Login Failed. Status: $($_.Exception.Response.StatusCode)"
    
    # Read Error Body
    $stream = $_.Exception.Response.GetResponseStream()
    $reader = New-Object System.IO.StreamReader($stream)
    $errorBody = $reader.ReadToEnd()
    Write-Host "Error Body: $errorBody"
    exit
}

$headers = @{
    Authorization = "Bearer $token"
}

try {
    $complaints = Invoke-RestMethod -Uri "$baseUrl/admin/complaints" -Method Get -Headers $headers -ErrorAction Stop
    
    $target = $complaints | Where-Object { $_.complaintNo -eq "CMP-46-4124" }
    
    if ($target) {
        Write-Host "Complaint Found: $($target.complaintNo)"
        Write-Host "Description: $($target.description)"
        Write-Host "Status: $($target.status)"
        
        # Checking photoUrl
        if ($target.photoUrl) {
            Write-Host "Main PhotoUrl: $($target.photoUrl)"
        }

        # Check Attachments List
        if ($target.attachments) {
            Write-Host "Attachment Count: $($target.attachments.Count)"
            foreach ($att in $target.attachments) {
                Write-Host " - ID: $($att.id), URL: $($att.url)"
            }
        }
        else {
            Write-Host "Attachment Count: 0"
        }
    }
    else {
        Write-Host "Complaint CMP-46-4124 NOT FOUND."
    }
}
catch {
    Write-Host "Error Fetching Complaints: $_"
}
