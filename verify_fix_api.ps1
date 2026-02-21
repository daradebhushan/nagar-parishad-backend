$headers = @{ "Content-Type" = "application/json" }
$body = @{ email = "daradebhushan15+depthead@gmail.com"; password = "password" } | ConvertTo-Json
$baseUrl = "http://localhost:8080/api"

try {
    Write-Host "Attempting Login..."
    $loginRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Headers $headers -Body $body
    Write-Host "Response: $($loginRes | ConvertTo-Json -Depth 5)"
    $token = $loginRes.data.token
    
    if (-not $token) {
        Write-Error "Login failed, no token received."
        exit
    }
    Write-Host "Login Success."
    
    $authHeaders = @{ "Authorization" = "Bearer $token" }
    
    # Verify Departments
    Write-Host "`nVerifying Departments Access..."
    try {
        $deptRes = Invoke-RestMethod -Uri "$baseUrl/admin/departments" -Method Get -Headers $authHeaders
        $depts = $deptRes.data.content
        Write-Host "Departments Count: $($depts.Count)"
        if ($depts.Count -ge 0) { Write-Host "SUCCESS: Access Granted to Departments" }
    }
    catch {
        Write-Host "FAILED: Department Check Failed: $_"
    }

    # Verify Users
    Write-Host "`nVerifying Users Access..."
    try {
        $userRes = Invoke-RestMethod -Uri "$baseUrl/admin/users" -Method Get -Headers $authHeaders
        $users = $userRes.data.content
        Write-Host "Users Count: $($users.Count)"
        if ($users.Count -ge 0) { Write-Host "SUCCESS: Access Granted to Users" }
    }
    catch {
        Write-Host "FAILED: User Check Failed: $_"
    }

}
catch {
    Write-Host "FATAL: Script Failed: $_"
}
