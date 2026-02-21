# debug_users.ps1
$authUrl = "http://localhost:8080/api/auth/login"
$usersUrl = "http://localhost:8080/api/admin/users"
$body = @{ email = "autoadmin@test.com"; password = "password123" } | ConvertTo-Json

try {
    $login = Invoke-RestMethod -Uri $authUrl -Method Post -Body $body -ContentType "application/json"
    $token = $login.data.token
    $headers = @{ Authorization = "Bearer $token" }

    Write-Host "Fetching Users..."
    $res = Invoke-RestMethod -Uri $usersUrl -Method Get -Headers $headers
    
    Write-Host "Response Type: $($res.GetType().Name)"
    Write-Host "Response Data Type: $($res.data.GetType().Name)"
    
    if ($res.data.content) {
        Write-Host "Response appears to be Paginated (found .data.content)."
        $users = $res.data.content
    }
    else {
        Write-Host "Response appears to be a List (using .data)."
        $users = $res.data
    }

    Write-Host "First 3 Users:"
    $users | Select-Object -First 3 | ConvertTo-Json -Depth 5
}
catch {
    Write-Host "Error: $_"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        Write-Host "Body: $($reader.ReadToEnd())"
    }
}
