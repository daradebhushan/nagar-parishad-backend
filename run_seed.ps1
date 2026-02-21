$loginUrl = "http://localhost:8080/api/auth/login"
$seedUrl = "http://localhost:8080/api/admin/seeder/seed-types"
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
    
    Write-Host "Running Seeder..."
    $seedResponse = Invoke-RestMethod -Uri $seedUrl -Method Post -Headers $headers
    
    Write-Host "Seeder Output:"
    Write-Host $seedResponse
}
catch {
    Write-Host "Error: $_"
}
