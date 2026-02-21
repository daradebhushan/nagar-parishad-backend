# verify_data_visibility.ps1
$baseUrl = "https://repayable-kenisha-inapprehensively.ngrok-free.dev/api"
$adminCreds = @{ email = "daradebhushan15+admin@gmail.com"; password = "Bbd@1415" }

Write-Host "Logging in as Admin..."
try {
    $login = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body ($adminCreds | ConvertTo-Json) -ContentType "application/json"
    $token = $login.data.token
    $headers = @{ Authorization = "Bearer $token" }

    # Users
    Write-Host "Fetching /auth/profile to get My ID..."
    # Assuming valid endpoint, or just parse from token?
    # Let's try /api/auth/me if exists, or check 'admin/users/profile' ?
    # Actually, let's just use what we have.
    $usersRes = Invoke-RestMethod -Uri "$baseUrl/admin/users?page=0&size=10" -Method Get -Headers $headers
    $uCount = if ($usersRes.data.content) { $usersRes.data.content.Count } else { $usersRes.data.Count }
    Write-Host "Visible Users: $uCount"
    if ($uCount -eq 0) {
        Write-Host "WARNING: No users found. Debugging contents:"
        Write-Host ($usersRes | ConvertTo-Json -Depth 2)
    }

    # Complaints
    $compRes = Invoke-RestMethod -Uri "$baseUrl/admin/complaints" -Method Get -Headers $headers
    Write-Host "Visible Complaints: $($compRes.Count)"
    if ($compRes.Count -gt 0) {
        Write-Host "Complaint[0]: $($compRes[0].description)"
    }
    else {
        Write-Host "WARNING: No complaints found."
    }

}
catch {
    Write-Host "Error: $_"
}
