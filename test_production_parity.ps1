$localUrl = "http://127.0.0.1:8080/api"
$prodUrl = "https://townseva.in/api"
$logFile = "d:\anti gravity\backend\parity_results.txt"

function Log-Output($message) {
    Write-Output $message
    Add-Content -Path $logFile -Value $message
}

Clear-Content -Path $logFile -ErrorAction SilentlyContinue

Log-Output "--- E2E PRODUCTION VS LOCAL REGRESSION MATRIX ---`n"

$ownerEmail = "owner@govt.in"
$ownerPassword = "password"
$ownerBody = @{ email = $ownerEmail; password = $ownerPassword } | ConvertTo-Json

# --- 1. AUTHENTICATION ---
Log-Output "Phase 1: Authenticating to both environments..."

try {
    $localLogin = Invoke-RestMethod -Uri "$localUrl/auth/login" -Method Post -Body $ownerBody -ContentType "application/json"
    $localToken = $localLogin.data.token
    Log-Output "[LOCAL] SUCCESS: Authenticated System Owner."
}
catch {
    Log-Output "[LOCAL] FAILED: Authentication - $($_.Exception.Message)"
}

try {
    $prodLogin = Invoke-RestMethod -Uri "$prodUrl/auth/login" -Method Post -Body $ownerBody -ContentType "application/json"
    $prodToken = $prodLogin.data.token
    Log-Output "[PROD]  SUCCESS: Authenticated System Owner."
}
catch {
    Log-Output "[PROD]  FAILED: Authentication - $($_.Exception.Message)"
}

$headersLocal = @{ Authorization = "Bearer $localToken" }
$headersProd = @{ Authorization = "Bearer $prodToken" }

# --- 2. DEPARTMENTS READ ---
Log-Output "`nPhase 2: Fetching Active Departments (Public API)..."

try {
    $localDepts = Invoke-RestMethod -Uri "$localUrl/public/departments" -Method Get
    Log-Output "[LOCAL] SUCCESS: Departments fetched. Count: $($localDepts.Count)"
}
catch {
    Log-Output "[LOCAL] FAILED: Departments - $($_.Exception.Message)"
}

try {
    $prodDepts = Invoke-RestMethod -Uri "$prodUrl/public/departments" -Method Get
    Log-Output "[PROD]  SUCCESS: Departments fetched. Count: $($prodDepts.Count)"
}
catch {
    Log-Output "[PROD]  FAILED: Departments - $($_.Exception.Message)"
}

# --- 3. COMPLAINTS READ ADMIN ---
Log-Output "`nPhase 3: Fetching Complaints (Admin API)..."

try {
    $localComp = Invoke-RestMethod -Uri "$localUrl/admin/complaints?size=1" -Method Get -Headers $headersLocal
    Log-Output "[LOCAL] SUCCESS: Complaints fetched. Total: $($localComp.data.totalElements)"
}
catch {
    Log-Output "[LOCAL] FAILED: Complaints - $($_.Exception.Message)"
}

try {
    $prodComp = Invoke-RestMethod -Uri "$prodUrl/admin/complaints?size=1" -Method Get -Headers $headersProd
    Log-Output "[PROD]  SUCCESS: Complaints fetched. Total: $($prodComp.data.totalElements)"
}
catch {
    Log-Output "[PROD]  FAILED: Complaints - $($_.Exception.Message)"
}

# --- 4. DATA STATISTICS READ ---
Log-Output "`nPhase 4: Fetching Global Dashboard Stats..."

try {
    $localStats = Invoke-RestMethod -Uri "$localUrl/dashboard/stats" -Method Get -Headers $headersLocal
    Log-Output "[LOCAL] SUCCESS: Stats fetched. Task Count: $($localStats.data.totalTasks)"
}
catch {
    Log-Output "[LOCAL] FAILED: Stats - $($_.Exception.Message)"
}

try {
    $prodStats = Invoke-RestMethod -Uri "$prodUrl/dashboard/stats" -Method Get -Headers $headersProd
    Log-Output "[PROD]  SUCCESS: Stats fetched. Task Count: $($prodStats.data.totalTasks)"
}
catch {
    Log-Output "[PROD]  FAILED: Stats - $($_.Exception.Message)"
}

# --- 5. USERS READ (The known 400 failure point) ---
Log-Output "`nPhase 5: Fetching User Index (Admin API)..."

try {
    $localUsers = Invoke-RestMethod -Uri "$localUrl/admin/users?size=5" -Method Get -Headers $headersLocal
    Log-Output "[LOCAL] SUCCESS: Users fetched. Returned objects: $($localUsers.data.content.Count)"
}
catch {
    Log-Output "[LOCAL] FAILED: Users - $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        Log-Output "[LOCAL] JSON RESPONSE: $($reader.ReadToEnd())"
    }
}

try {
    $prodUsers = Invoke-RestMethod -Uri "$prodUrl/admin/users?size=5" -Method Get -Headers $headersProd
    Log-Output "[PROD]  SUCCESS: Users fetched. Returned objects: $($prodUsers.data.content.Count)"
}
catch {
    Log-Output "[PROD]  FAILED: Users - $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        Log-Output "[PROD]  JSON RESPONSE: $($reader.ReadToEnd())"
    }
}

# --- 6. USER CREATION DRIFT TEST ---
Log-Output "`nPhase 6: Single-Post Write Test (User Creation)..."
$testEmail = "parity_$((Get-Date).Ticks)@test.com"
$testUserBody = @{
    name     = "Parity Test User"
    email    = $testEmail
    password = "password123"
    mobile   = "1231231231"
    role     = "STAFF"
    active   = $true
} | ConvertTo-Json

try {
    $localNewUser = Invoke-RestMethod -Uri "$localUrl/admin/users" -Method Post -Body $testUserBody -ContentType "application/json" -Headers $headersLocal
    Log-Output "[LOCAL] SUCCESS: Write User (ID: $($localNewUser.data.id))"
    Invoke-RestMethod -Uri "$localUrl/admin/users/$($localNewUser.data.id)" -Method Delete -Headers $headersLocal | Out-Null
}
catch {
    Log-Output "[LOCAL] FAILED: Write User - $($_.Exception.Message)"
}

try {
    $prodNewUser = Invoke-RestMethod -Uri "$prodUrl/admin/users" -Method Post -Body $testUserBody -ContentType "application/json" -Headers $headersProd
    Log-Output "[PROD]  SUCCESS: Write User (ID: $($prodNewUser.data.id))"
    Invoke-RestMethod -Uri "$prodUrl/admin/users/$($prodNewUser.data.id)" -Method Delete -Headers $headersProd | Out-Null
}
catch {
    Log-Output "[PROD]  FAILED: Write User - $($_.Exception.Message)"
}

Log-Output "`n--- DRIFT MATRIX COMPLETE ---"
