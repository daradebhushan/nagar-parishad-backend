$url = "http://localhost:8080/api/auth/setup-owner"
$body = @{
    name     = "Test Admin"
    mobile   = "9999999999"
    email    = "daradebhushan15+admin@gmail.com"
    password = "Bbd@1415"
    role     = "ADMIN"
} | ConvertTo-Json

try {
    Invoke-RestMethod -Uri $url -Method Post -Body $body -ContentType "application/json" -ErrorAction Stop
    Write-Host "User 'daradebhushan15+admin@gmail.com' created successfully."
}
catch {
    Write-Host "Failed or user already exists: $_"
}
