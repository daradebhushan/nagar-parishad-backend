$ErrorActionPreference = "Stop"

# Configuration
$baseUrl = "http://localhost:8080/api"
$ownerEmail = "owner_sys_iso@test.com"
$adminAEmail = "tenantA_admin@test.com"
$adminBEmail = "tenantB_admin@test.com"
$password = "password123"

function Get-Token {
    param ($email, $password)
    $loginUrl = "$baseUrl/auth/login"
    $body = @{ email = $email; password = $password } | ConvertTo-Json
    try {
        $response = Invoke-RestMethod -Uri $loginUrl -Method Post -Body $body -ContentType "application/json" -ErrorAction Stop
        return $response.data.token
    }
    catch {
        throw "Login failed for $email. $_"
    }
}

function Setup-Owner {
    $url = "$baseUrl/auth/setup-owner"
    $body = @{ name = "System Owner"; email = $ownerEmail; password = $password; mobile = "0000000000"; role = "OWNER" } | ConvertTo-Json
    try {
        Invoke-RestMethod -Uri $url -Method Post -Body $body -ContentType "application/json" -ErrorAction SilentlyContinue
        Write-Host "   Owner Setup/Exists."
    }
    catch {
        Write-Host "   Owner setup skipped (likely exists)."
    }
}

function Create-TenantAdmin {
    param ($ownerToken, $email, $name)
    $url = "$baseUrl/owner/create-admin"
    $headers = @{ Authorization = "Bearer $ownerToken" }
    
    # Check if exists first by trying to login? No, just try create
    # If exists, we catch error.
    
    $body = @{ name = $name; email = $email; password = $password; mobile = "1234567890"; role = "ADMIN" } | ConvertTo-Json
    
    try {
        Invoke-RestMethod -Uri $url -Method Post -Headers $headers -Body $body -ContentType "application/json" -ErrorAction SilentlyContinue
        Write-Host "   Created Admin: $email"
    }
    catch {
        Write-Host "   Admin likely exists: $email ($($_.Exception.Message))"
    }
}

function Create-Task {
    param ($token, $title)
    $url = "$baseUrl/tasks/create"
    $headers = @{ Authorization = "Bearer $token" }
    $body = @{
        title       = $title
        description = "Isolation Test Task"
        priority    = "HIGH"
        status      = "TO_DO"
        dueDate     = (Get-Date).AddDays(1).ToString("yyyy-MM-ddTHH:mm:ss")
    } | ConvertTo-Json
    
    $res = Invoke-RestMethod -Uri $url -Method Post -Headers $headers -Body $body -ContentType "application/json"
    return $res.data.id
}

function Get-Tasks {
    param ($token)
    $url = "$baseUrl/tasks"
    $headers = @{ Authorization = "Bearer $token" }
    $res = Invoke-RestMethod -Uri $url -Method Get -Headers $headers
    if ($res.data.content) { return $res.data.content }
    return $res.data
}

Write-Host "--- Starting Isolation Verification V2 ---"

# 1. Setup Owner
Write-Host "1. Setting up Owner..."
Setup-Owner
$ownerToken = Get-Token -email $ownerEmail -password $password

# 2. Create Tenant Admins
Write-Host "2. Creating Tenant Admins..."
Create-TenantAdmin -ownerToken $ownerToken -email $adminAEmail -name "Tenant A Admin"
Create-TenantAdmin -ownerToken $ownerToken -email $adminBEmail -name "Tenant B Admin"

# 3. Authenticate Admins
Write-Host "3. Authenticating Tenants..."
$tokenA = Get-Token -email $adminAEmail -password $password
$tokenB = Get-Token -email $adminBEmail -password $password

# 4. Create Tasks
Write-Host "4. Creating Tasks..."
$taskAId = Create-Task -token $tokenA -title "Task For Tenant A"
$taskBId = Create-Task -token $tokenB -title "Task For Tenant B"
Write-Host "   Tasks Created (A: $taskAId, B: $taskBId)"

# 5. Verify Isolation
Write-Host "5. Verifying Isolation for Tenant A..."
$tasksA = Get-Tasks -token $tokenA
$tasksAIds = $tasksA | ForEach-Object { $_.id }

if ($tasksAIds -contains $taskAId -and $tasksAIds -notcontains $taskBId) {
    Write-Host "   [PASS] Tenant A sees Task A and NOT Task B."
}
else {
    Write-Host "   TASKS VISIBLE: $($tasksAIds -join ', ')"
    Write-Error "   [FAIL] Tenant A visibility incorrect."
}

Write-Host "6. Verifying Isolation for Tenant B..."
$tasksB = Get-Tasks -token $tokenB
$tasksBIds = $tasksB | ForEach-Object { $_.id }

if ($tasksBIds -contains $taskBId -and $tasksBIds -notcontains $taskAId) {
    Write-Host "   [PASS] Tenant B sees Task B and NOT Task A."
}
else {
    Write-Host "   TASKS VISIBLE: $($tasksBIds -join ', ')"
    Write-Error "   [FAIL] Tenant B visibility incorrect."
}

Write-Host "--- Isolation Logic Verified Successfully ---"
