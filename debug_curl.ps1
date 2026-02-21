# debug_curl.ps1
$url = "https://repayable-kenisha-inapprehensively.ngrok-free.dev/api"
$ownerCreds = @{ email = "daradebhushan15+owner@gmail.com"; password = "Bbd@1415" }

# Login and save token
$login = Invoke-RestMethod -Uri "$url/auth/login" -Method Post -Body ($ownerCreds | ConvertTo-Json) -ContentType "application/json"
$token = $login.data.token

Write-Host "Token: $token"
$token | Out-File "token.txt"

# Call curl directly for users (PowerShell 'curl' is alias for Invoke-WebRequest, so use external)
# We can use Invoke-WebRequest to see raw content
$response = Invoke-WebRequest -Uri "$url/admin/users?page=0&size=10" -Headers @{ Authorization = "Bearer $token" }
Write-Host "Raw Content:"
Write-Host $response.Content
