$baseUrl = "http://localhost:8080/api"
$adminLogin = @{ email = "admin@test.com"; password = "password" } | ConvertTo-Json
$adminRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $adminLogin -ContentType "application/json"
$token = $adminRes.data.token
$headers = @{ Authorization = "Bearer $token" }

# Simulate Frontend Request: Page 0, Size 10
$tasksRes = Invoke-RestMethod -Uri "$baseUrl/tasks?page=0&size=10" -Method Get -Headers $headers
Write-Host "Task List JSON:"
$tasksRes.data | ConvertTo-Json -Depth 10
