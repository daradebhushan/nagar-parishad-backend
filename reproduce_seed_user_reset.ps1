$baseUrl = "http://localhost:8080/api/auth"
$testEmail = "head@sanitation.in" # Seed user
$password = "password" # Seed password
$newPassword = "newpassword789"

Write-Host "Target User: $testEmail"

# 1. Login with Old Password (Verify Seed State)
$loginUrl = "$baseUrl/login"
$loginBody = @{ email = $testEmail; password = $password } | ConvertTo-Json
try {
    Write-Host "1. Logging in with OLD (Seed) password..."
    $loginRes = Invoke-RestMethod -Uri $loginUrl -Method Post -Body $loginBody -ContentType "application/json"
    Write-Host "Login Success with Old Password."
}
catch {
    Write-Host "Login Failed with Old Password: $_"
    # If this fails, maybe I already reset it in previous manual tests?
    # If so, we might need to manually reset it back or use another user.
    # Let's try 'ramesh@sanitation.in' if this fails.
}

# 2. Forgot Password
$forgotUrl = "$baseUrl/forgot-password"
$forgotBody = @{ email = $testEmail } | ConvertTo-Json
Write-Host "2. Requesting Password Reset..."
try {
    Invoke-RestMethod -Uri $forgotUrl -Method Post -Body $forgotBody -ContentType "application/json"
}
catch {
    Write-Host "Forgot Password Failed: $_"
    exit
}

# 3. Get OTP
Start-Sleep -Seconds 2
$logFile = "d:\anti gravity\backend\backend_debug_session.log"
$otpLine = Get-Content $logFile -Tail 200 | Where-Object { $_ -match "Reset OTP for $testEmail" } | Select-Object -Last 1

if (-not $otpLine) {
    Write-Error "Could not find OTP in logs."
    exit
}
$otp = $otpLine -replace ".*: ", ""
Write-Host "Found OTP: $otp"

# 4. Reset Password
$resetUrl = "$baseUrl/reset-password"
$resetBody = @{
    email       = $testEmail
    otp         = $otp
    newPassword = $newPassword 
} | ConvertTo-Json

Write-Host "4. Reseting Password..."
try {
    Invoke-RestMethod -Uri $resetUrl -Method Post -Body $resetBody -ContentType "application/json"
    Write-Host "Password Reset Success."
}
catch {
    Write-Host "Password Reset Failed: $_"
    exit
}

# 5. Login with New Password
$loginBodyNew = @{ email = $testEmail; password = $newPassword } | ConvertTo-Json
try {
    Write-Host "5. Attempting Login with NEW password..."
    $loginResNew = Invoke-RestMethod -Uri $loginUrl -Method Post -Body $loginBodyNew -ContentType "application/json"
    if ($loginResNew.success) {
        Write-Host "SUCCESS: Login worked with new password!"
    }
    else {
        Write-Host "FAILURE: Login returned success=false"
    }
}
catch {
    Write-Host "FAILURE: Login threw exception with new password: $_"
}
