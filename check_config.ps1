try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/public/whatsapp/check-config" -Method Get -ErrorAction Stop
    Write-Host "Config: $response"
}
catch {
    Write-Error "Check Failed: $_"
}
