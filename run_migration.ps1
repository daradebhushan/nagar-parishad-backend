$loginUrl = "http://localhost:8080/api/auth/login"
$migrationUrl = "http://localhost:8080/api/admin/migration/fix-descriptions"
$cred = @{
    email    = "admin@nagarparishad.in"
    password = "password"
}
$body = $cred | ConvertTo-Json

try {
    Write-Host "Logging in..."
    $response = Invoke-RestMethod -Uri $loginUrl -Method Post -Body $body -ContentType "application/json"
    
    if ($response.success -eq $true) {
        $token = $response.data.token
        Write-Host "Login Successful. Token received."
        
        $headers = @{
            Authorization = "Bearer $token"
        }
        
        Write-Host "Running Migration..."
        $migResponse = Invoke-RestMethod -Uri $migrationUrl -Method Post -Headers $headers
        
        Write-Host "Migration Output:"
        Write-Host $migResponse
    }
    else {
        Write-Error "Login Failed: $($response.message)"
    }
}
catch {
    Write-Error "Error: $_"
    Write-Host "Exception Details: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        Write-Host "Response Body: $($reader.ReadToEnd())"
    }
}
