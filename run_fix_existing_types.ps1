$loginUrl = "http://localhost:8080/api/auth/login"
$fixUrl = "http://localhost:8080/api/admin/migration/fix-types"
$cred = @{
    email    = "admin@nagarparishad.in"
    password = "password"
}
$body = $cred | ConvertTo-Json

try {
    Write-Host "Logging in..."
    $response = Invoke-RestMethod -Uri $loginUrl -Method Post -Body $body -ContentType "application/json"
    $token = $response.data.token
    $headers = @{ Authorization = "Bearer $token" }
    
    Write-Host "Running Type Fix..."
    $fixResponse = Invoke-RestMethod -Uri $fixUrl -Method Post -Headers $headers
    
    Write-Host "Fix Output:"
    Write-Host $fixResponse
}
catch {
    Write-Host "Error: $_"
}
