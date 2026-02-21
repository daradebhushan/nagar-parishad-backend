$loginUrl = "http://localhost:8080/api/auth/login"
$deptUrl = "http://localhost:8080/api/admin/departments"
$cred = @{ email = "admin@nagarparishad.in"; password = "password" }
$body = $cred | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri $loginUrl -Method Post -Body $body -ContentType "application/json"
    $token = $response.data.token
    $headers = @{ Authorization = "Bearer $token" }
    
    $d = Invoke-RestMethod -Uri $deptUrl -Method Get -Headers $headers
    $d.data.content | Select-Object id, name | Format-Table -AutoSize
}
catch {
    Write-Host "Error: $_"
}
