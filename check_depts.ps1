$loginUrl = "http://localhost:8080/api/auth/login"
$deptUrl = "http://localhost:8080/api/admin/departments"

$loginPayload = @{
    email    = "admin@nagarparishad.in"
    password = "password"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri $loginUrl -Method Post -Body $loginPayload -ContentType "application/json"
    $token = $response.data.token
    $headers = @{ Authorization = "Bearer $token" }
    
    $depts = Invoke-RestMethod -Uri $deptUrl -Method Get -Headers $headers
    Write-Host "Departments:"
    Write-Host ($depts.data | ConvertTo-Json -Depth 5)
}
catch { Write-Host $_ }
