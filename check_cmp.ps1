$token = (Get-Content login.json -Raw | ConvertFrom-Json).data.accessToken
$headers = @{ "Authorization" = "Bearer $token" }
try {
    $res = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/complaints" -Headers $headers
    $target = $res | Where-Object { $_.complaintNo -eq "CMP-46-4124" }
    
    if ($target) {
        Write-Host "Complaint Found: $($target.complaintNo)"
        Write-Host "Attachment Count: $($target.attachments.Count)"
        $target.attachments | ForEach-Object { Write-Host " - $($_.url)" }
    }
    else {
        Write-Host "Complaint Not Found!"
    }
}
catch {
    Write-Host "Error: $_"
}
