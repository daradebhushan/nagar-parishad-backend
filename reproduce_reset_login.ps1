$baseUrl = "http://localhost:8080/api/auth"
$testEmail = "test.reset" + (Get-Random) + "@example.com"
$password = "password123"
$newPassword = "newpassword456"

# 1. Register User (Owner setup endpoint is open?) 
# Alternatively use a known user. Let's try to register if possible, or use standard signup.
# AuthController has /setup-owner and /register (wait, AuthService has registerUser but AuthController usually exposes it).
# Looking at AuthController, I only see /setup-owner and /login, /forgot-password...
# Wait, where is the general registration? 
# Ah, usually registration is protected or maybe I missed it in AuthController?
# Let's check AuthController again.

# ... Checking AuthController ... 
# /setup-owner is there.
# But for regular users? AdminController?
# Let's assume I can use an existing user or create one via Admin endpoint if I have token.
# To keep it simple, I'll use the "head@sanitation.in" if I can, OR just create a new Owner via /setup-owner just for testing.

$registerUrl = "$baseUrl/setup-owner"
$registerBody = @{
    name     = "Test Reset User"
    email    = $testEmail
    password = $password
    mobile   = "1234567890"
    role     = "OWNER" 
} | ConvertTo-Json

Write-Host "1. Registering User: $testEmail"
try {
    $regRes = Invoke-RestMethod -Uri $registerUrl -Method Post -Body $registerBody -ContentType "application/json"
    Write-Host "Registration Success: $($regRes.message)"
}
catch {
    Write-Host "Registration Failed/Skipped: $_"
    # If failed, maybe user exists, try to continue
}

# 2. Login with Old Password
$loginUrl = "$baseUrl/login"
$loginBody = @{ email = $testEmail; password = $password } | ConvertTo-Json
try {
    Write-Host "2. Logging in with OLD password..."
    $loginRes = Invoke-RestMethod -Uri $loginUrl -Method Post -Body $loginBody -ContentType "application/json"
    Write-Host "Login Success with Old Password."
}
catch {
    Write-Host "Login Failed with Old Password: $_"
    exit
}

# 3. Forgot Password
$forgotUrl = "$baseUrl/forgot-password"
$forgotBody = @{ email = $testEmail } | ConvertTo-Json
Write-Host "3. Requesting Password Reset..."
Invoke-RestMethod -Uri $forgotUrl -Method Post -Body $forgotBody -ContentType "application/json"

# 4. Get OTP (Wait a bit for log to flush)
Start-Sleep -Seconds 2
$logFile = "d:\anti gravity\backend\backend_debug_session.log"
# We need to find the latest OTP line
$otpLine = Get-Content $logFile -Tail 100 | Where-Object { $_ -match "Reset OTP for $testEmail" } | Select-Object -Last 1

if (-not $otpLine) {
    Write-Error "Could not find OTP in logs."
    exit
}
$otp = $otpLine -replace ".*: ", ""
Write-Host "Found OTP: $otp"

# 5. Reset Password
$resetUrl = "$baseUrl/reset-password"
$resetBody = @{
    email       = $testEmail
    otp         = $otp
    newPassword = $newPassword 
} | ConvertTo-Json

Write-Host "5. Reseting Password..."
try {
    Invoke-RestMethod -Uri $resetUrl -Method Post -Body $resetBody -ContentType "application/json"
    Write-Host "Password Reset Success."
}
catch {
    Write-Host "Password Reset Failed: $_"
    exit
}

# 6. Login with New Password
$loginBodyNew = @{ email = $testEmail; password = $newPassword } | ConvertTo-Json
try {
    Write-Host "6. Attempting Login with NEW password..."
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
    # Debug: Try old password again?
    try {
        Invoke-RestMethod -Uri $loginUrl -Method Post -Body $loginBody -ContentType "application/json"
        Write-Host "WTF: Login still works with OLD password?"
    }
    catch {
        Write-Host "Old password also failed (Expected)."
    }
}
