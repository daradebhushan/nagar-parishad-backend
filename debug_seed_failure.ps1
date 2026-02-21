# debug_seed_failure.ps1
$baseUrl = "https://repayable-kenisha-inapprehensively.ngrok-free.dev/api"
$ownerCreds = @{ email = "daradebhushan15+owner@gmail.com"; password = "Bbd@1415" }

# Login
$login = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body ($ownerCreds | ConvertTo-Json) -ContentType "application/json"
$token = $login.data.token
$headers = @{ Authorization = "Bearer $token" }

# Fetch Dept/Desig
$deptRes = Invoke-RestMethod -Uri "$baseUrl/public/departments" -Method Get -Headers $headers
$depts = if ($deptRes.data) { $deptRes.data } else { $deptRes }
$deptId = if ($depts.Count -gt 0) { $depts[0].id } else { 1 } # Default to 1 if empty

$desigRes = Invoke-RestMethod -Uri "$baseUrl/public/designations" -Method Get -Headers $headers
$desigs = if ($desigRes.data) { $desigRes.data } else { $desigRes }
$desigId = if ($desigs.Count -gt 0) { $desigs[0].id } else { 1 } # Default to 1 if empty

Write-Host "Using Dept ID: $deptId, Desig ID: $desigId"

# Create Staff Body
$staffBody = @{
    name          = "Debug Staff"
    email         = "debug.staff@demo.com"
    password      = "password"
    mobile        = "1234567890"
    role          = "STAFF"
    active        = $true
    departmentId  = $deptId
    designationId = $desigId
} | ConvertTo-Json

try {
    Invoke-RestMethod -Uri "$baseUrl/auth/signup" -Method Post -Body $staffBody -ContentType "application/json" -Headers $headers
    Write-Host "Staff Created Successfully."
}
catch {
    Write-Host "Creation FAILED: $_"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        Write-Host "Response Body: $($reader.ReadToEnd())"
    }
}
