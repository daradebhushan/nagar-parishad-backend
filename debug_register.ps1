try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/setup-owner" -Method Post -ContentType "application/json" -Body '{"name": "New Owner 2", "email": "newowner2@test.com", "password": "password123", "mobile": "1234567891", "role": "OWNER"}'
    Write-Host "Success:"
    $response | ConvertTo-Json
}
catch {
    Write-Host "Error:"
    if ($_.Exception.Response) {
        Write-Host $_.Exception.Response.StatusCode
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $errorBody = $reader.ReadToEnd()
        Write-Host $errorBody
    }
    else {
        Write-Host $_.Exception.Message
    }
}
