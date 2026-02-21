$ErrorActionPreference = "Stop"

function Assert-Success {
    param (
        [string]$Step,
        [int]$StatusCode,
        [int]$ExpectedCode = 200
    )
    if ($StatusCode -eq $ExpectedCode) {
        Write-Host "[PASS] $Step" -ForegroundColor Green
    }
    else {
        Write-Host "[FAIL] $Step - Expected $ExpectedCode but got $StatusCode" -ForegroundColor Red
        exit 1
    }
}

# 1. Login as Owner to get Token
Write-Host "Logging in as Owner..."
$loginUrl = "http://localhost:8080/api/auth/login"
$loginBody = @{
    email    = "bhushandarade1407@gmail.com"
    password = "admin123"
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri $loginUrl -Method Post -Body $loginBody -ContentType "application/json"
    $token = $loginResponse.data.token
    $ownerId = $loginResponse.data.id
    Write-Host "Token: $token"
    Assert-Success "Login" 200
}
catch {
    Write-Host "[FAIL] Login failed: $_" -ForegroundColor Red
    exit 1
}

$headers = @{
    Authorization = "Bearer $token"
}

# 2. Create a Test Staff User
Write-Host "Creating Test Staff User..."
$createStaffUrl = "http://localhost:8080/api/admin/users"
$randomNum = Get-Random -Minimum 1000 -Maximum 9999
$staffEmail = "staff_update_test_$randomNum@test.com"
# Need department ID. AdminController /admin/users creates user.
# If we don't pass Dept ID, it might fail if logic requires it for Staff?
# AuthService: if Role == STAFF, finds Dept Head.
# Let's try creating a user without Dept if allowed, or we fetch dept first.
# For simplicity, let's assume Dept 1 exists (DataSeeder creates one).

$createStaffBody = @{
    name         = "Test Staff For Update"
    email        = $staffEmail
    password     = "password"
    mobile       = "9876543210"
    role         = "STAFF"
    departmentId = 1
} | ConvertTo-Json

try {
    $createStaffRes = Invoke-RestMethod -Uri $createStaffUrl -Method Post -Body $createStaffBody -Headers $headers -ContentType "application/json"
    $staffId = $createStaffRes.data.id
    Assert-Success "Create Staff" 200
}
catch {
    Write-Host "[FAIL] Create Staff failed: $_" -ForegroundColor Red
    # Print error details
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader $_.Exception.Response.GetResponseStream()
        Write-Host "Response: $($reader.ReadToEnd())"
    }
    exit 1
}

# 3. Update the Staff User (Change Name and Mobile)
Write-Host "Updating Staff User..."
$updateUrl = "http://localhost:8080/api/admin/users/$staffId"
$updateBody = @{
    name   = "Updated Staff Name"
    mobile = "1234567890"
} | ConvertTo-Json

try {
    $updateRes = Invoke-RestMethod -Uri $updateUrl -Method Put -Body $updateBody -Headers $headers -ContentType "application/json"
    
    if ($updateRes.data.name -eq "Updated Staff Name" -and $updateRes.data.mobile -eq "1234567890") {
        Write-Host "[PASS] Update Staff Name and Mobile" -ForegroundColor Green
    }
    else {
        Write-Host "[FAIL] Update Staff Name/Mobile match failed" -ForegroundColor Red
        Write-Host "Got: $($updateRes.data | ConvertTo-Json)"
        exit 1
    }
}
catch {
    Write-Host "[FAIL] Update Staff failed: $_" -ForegroundColor Red
    exit 1
}

# 4. Verify Password Update (Login with new password)
Write-Host "Updating Password..."
$newPassword = "newpassword123"
$updatePassBody = @{
    password = $newPassword
} | ConvertTo-Json

try {
    $updatePassRes = Invoke-RestMethod -Uri $updateUrl -Method Put -Body $updatePassBody -Headers $headers -ContentType "application/json"
    Assert-Success "Update Password API Call" 200
}
catch {
    Write-Host "[FAIL] Update Password call failed: $_" -ForegroundColor Red
    exit 1
}

# 5. Verify Login with New Password
Write-Host "Verifying Login with New Password..."
$loginNewBody = @{
    email    = $staffEmail
    password = $newPassword
} | ConvertTo-Json

try {
    $loginNewRes = Invoke-RestMethod -Uri $loginUrl -Method Post -Body $loginNewBody -ContentType "application/json"
    Assert-Success "Login with New Password" 200
}
catch {
    Write-Host "[FAIL] Login with New Password failed: $_" -ForegroundColor Red
    exit 1
}

# Clean Access
# Ideally delete the user, but for now we leave it or relying on DB reset/random emails.
# Let's delete it to be clean.
Write-Host "Deleting Test User..."
$deleteUrl = "http://localhost:8080/api/admin/users/$staffId"
try {
    Invoke-RestMethod -Uri $deleteUrl -Method Delete -Headers $headers
    Assert-Success "Delete Test User" 200
}
catch {
    Write-Host "[WARN] Delete failed, manual cleanup might be needed."
}

Write-Host "All User Update Tests Passed!" -ForegroundColor Cyan
