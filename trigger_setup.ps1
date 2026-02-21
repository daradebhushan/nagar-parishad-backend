try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/public/whatsapp/setup-config" -Method Get -ErrorAction Stop
    Write-Host "Setup Result: $response"
}
catch {
    Write-Error "Setup Failed: $_"
}
