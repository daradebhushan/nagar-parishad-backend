# debug_login_v2.ps1
$url = "http://localhost:8080/api/auth/login"

function Test-Login ($email, $password) {
    Write-Host "Testing Login for: $email"
    $body = @{ email = $email; password = $password } | ConvertTo-Json
    
    try {
        $response = Invoke-RestMethod -Uri $url -Method Post -Body $body -ContentType "application/json"
        Write-Host "SUCCESS" -ForegroundColor Green
        $response | ConvertTo-Json -Depth 5
    }
    catch {
        Write-Host "FAILED: $($_.Exception.Message)" -ForegroundColor Red
        if ($_.Exception.Response) {
            $reader = New-Object System.IO.StreamReader $_.Exception.Response.GetResponseStream()
            $errBody = $reader.ReadToEnd()
            Write-Host "Error Body: $errBody" -ForegroundColor Yellow
        }
    }
    Write-Host "--------------------------------"
}

Test-Login "admin@nagarparishad.in" "admin"
Test-Login "autoadmin@test.com" "password123"
