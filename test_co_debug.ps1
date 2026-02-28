$baseUrl = "http://127.0.0.1:8080/api"
$ownerBody = @{ email = "owner@govt.in"; password = "password" } | ConvertTo-Json
Write-Host "Logging in Owner..."
$ownerData = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $ownerBody -ContentType "application/json"
$headersOwner = @{ Authorization = "Bearer $($ownerData.token)" }

Write-Host "Creating Admin User..."
$coBody = @{
    name     = "Test Chief Officer"
    email    = "co_test_debug@nagarparishad.in"
    password = "password123"
    mobile   = "9998887771"
    role     = "ADMIN"
    active   = $true
} | ConvertTo-Json

try {
    $res = Invoke-RestMethod -Uri "$baseUrl/admin/users" -Method Post -Body $coBody -ContentType "application/json" -Headers $headersOwner
    Write-Host "User Built!"
}
catch {
    Write-Host "HTTP ERROR STATUS: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        Write-Host "JSON RESPONSE: $($reader.ReadToEnd())"
    }
    else {
        Write-Host "No response body."
    }
}
