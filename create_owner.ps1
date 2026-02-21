$headers = @{
    "Content-Type" = "application/json"
}

$body = @{
    "name"     = "New Owner"
    "email"    = "newowner@test.com"
    "password" = "Password@123"
    "role"     = "OWNER"
    "mobile"   = "1234567890"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/auth/setup-owner" -Method Post -Headers $headers -Body $body
