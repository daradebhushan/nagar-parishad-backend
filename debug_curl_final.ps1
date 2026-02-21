# debug_curl_final.ps1
$url = "https://repayable-kenisha-inapprehensively.ngrok-free.dev/api"
$ownerCreds = @{ email = "daradebhushan15+admin@gmail.com"; password = "Bbd@1415" }

# Login
$login = Invoke-RestMethod -Uri "$url/auth/login" -Method Post -Body ($ownerCreds | ConvertTo-Json) -ContentType "application/json"
$token = $login.data.token
$headers = @{ Authorization = "Bearer $token" }

# Use external curl to ensure raw output
# Need to pass header carefully in PS
$authHeader = "Authorization: Bearer $token"
$endpoint = "$url/admin/users?page=0&size=10"

Write-Host "Calling $endpoint..."
curl -H $authHeader $endpoint -v
