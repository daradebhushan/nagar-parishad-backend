$login = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body (@{email = "admin@nagarparishad.in"; password = "password" } | ConvertTo-Json) -ContentType "application/json"
$token = $login.data.accessToken
$complaints = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/complaints" -Method Get -Headers @{Authorization = "Bearer $token" }
$target = $complaints | Where-Object { $_.citizenName -eq "Task Flow Verify" }
if ($target) { Write-Host "FOUND: $($target.complaintNo) - $($target.status)" } else { Write-Host "NOT FOUND" }
