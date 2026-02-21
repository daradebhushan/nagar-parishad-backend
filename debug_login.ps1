$ErrorActionPreference = "Stop"

# 1. Login
$loginUrl = "http://localhost:8080/api/auth/login"
$loginBody = @{
    email    = "admin@test.com"
    password = "password"
} | ConvertTo-Json

Write-Host "Logging in..."
try {
    $loginResponse = Invoke-RestMethod -Uri $loginUrl -Method Post -Body $loginBody -ContentType "application/json"
    Write-Host "Login Response: " ($loginResponse | ConvertTo-Json -Depth 5)
}
catch {
    Write-Error "Login failed: $_"
    exit 1
}

# Adjust property access based on actual response structure
$token = $loginResponse.data.accessToken
if (-not $token) {
    # Try alternate structure
    $token = $loginResponse.accessToken
}

if (-not $token) {
    Write-Error "No token received. Check response above."
    exit 1
}
Write-Host "Login successful. Token received."
