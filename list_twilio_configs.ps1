# list_twilio_configs.ps1
$url = "https://repayable-kenisha-inapprehensively.ngrok-free.dev/api"

Write-Host "Fetching Twilio Configs..."
try {
    # Public endpoint, no auth for diag (as per controller code)
    # Header bypass header is essential
    $headers = @{ "ngrok-skip-browser-warning" = "true" }
    $res = Invoke-RestMethod -Uri "$url/public/diag/configs" -Method Get -Headers $headers
    
    if ($res) {
        $res | ForEach-Object { Write-Host $_ }
    }
    else {
        Write-Host "No configs returned."
    }
}
catch {
    Write-Host "Fetch Failed: $_"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        Write-Host "Error Body: $($reader.ReadToEnd())"
    }
}
