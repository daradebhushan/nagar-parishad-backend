# create_staff_manual.ps1
$baseUrl = "https://repayable-kenisha-inapprehensively.ngrok-free.dev/api"
$adminCreds = @{ email = "daradebhushan15+admin@gmail.com"; password = "Bbd@1415" }

# 1. Login
Write-Host "Logging in..."
try {
    $login = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body ($adminCreds | ConvertTo-Json) -ContentType "application/json"
    $token = $login.data.token
    $headers = @{ Authorization = "Bearer $token" }
    Write-Host "Login Success."
}
catch {
    Write-Host "Login Failed: $_"
    exit
}

# 2. Get Dept
$deptRes = Invoke-RestMethod -Uri "$baseUrl/public/departments" -Method Get -Headers $headers
$deptId = $deptRes.data[0].id
$desigRes = Invoke-RestMethod -Uri "$baseUrl/public/designations" -Method Get -Headers $headers
$desigId = $desigRes.data[0].id

Write-Host "Dept: $deptId, Desig: $desigId"

# 3. Create Staff
$staffBody = @{
    name          = "Demo Staff Manual"
    email         = "manual.staff@demo.com"
    password      = "password"
    mobile        = "1122334455"
    role          = "STAFF"
    active        = $true
    departmentId  = $deptId
    designationId = $desigId
} | ConvertTo-Json

Write-Host "Sending Create Request..."
try {
    $res = Invoke-RestMethod -Uri "$baseUrl/admin/users" -Method Post -Body $staffBody -ContentType "application/json" -Headers $headers
    Write-Host "Response:"
    Write-Host ($res | ConvertTo-Json -Depth 5)
    
    if ($res.success) {
        Write-Host "Creation API claims SUCCESS."
    }
    else {
        Write-Host "Creation API claims FAILURE."
    }
}
catch {
    Write-Host "Creation Request FAILED with Exception:"
    Write-Host $_
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        Write-Host "Error Body: $($reader.ReadToEnd())"
    }
}
