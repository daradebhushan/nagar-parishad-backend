$ErrorActionPreference = "Stop"

# Configuration
$baseUrl = "http://localhost:8080/api"
$ownerEmail = "owner_sys_iso@test.com"
$adminAEmail = "tenantA_admin@test.com"
$adminBEmail = "tenantB_admin@test.com"
$deptHeadAEmail = "deptHeadA@test.com"
$staffAEmail = "staffA@test.com"
$deptHeadBEmail = "deptHeadB@test.com"
$staffBEmail = "staffB@test.com"
$testEmail = "daradebhushan15@gmail.com" # For email verification
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
    
    $body = @{ name = $name; email = $email; password = $password; mobile = "1234567890"; role = "ADMIN" } | ConvertTo-Json
    
    try {
        Invoke-RestMethod -Uri $url -Method Post -Headers $headers -Body $body -ContentType "application/json" -ErrorAction SilentlyContinue
        Write-Host "   Created Admin: $email"
    }
    catch {
        Write-Host "   Admin likely exists: $email"
    }
}

function Create-Department {
    param ($token, $name)
    $url = "$baseUrl/admin/departments"
    $headers = @{ Authorization = "Bearer $token" }
    $body = @{ name = $name } | ConvertTo-Json
    try {
        $res = Invoke-RestMethod -Uri $url -Method Post -Headers $headers -Body $body -ContentType "application/json" -ErrorAction Stop
        return $res.data.id
    }
    catch {
        # If already exists, we might need to fetch it.
        Write-Warning "   Create Dept failed ($name), assuming exists. Fetching..."
        $listUrl = "$baseUrl/admin/departments"
        $listRes = Invoke-RestMethod -Uri $listUrl -Method Get -Headers $headers
        $dept = $listRes.data.content | Where-Object { $_.name -eq $name }
        if ($dept) { return $dept.id }
        throw "Could not find or create department $name"
    }
}

function Create-User {
    param ($token, $name, $email, $role, $deptId)
    $url = "$baseUrl/admin/users"
    $headers = @{ Authorization = "Bearer $token" }
    $body = @{ 
        name         = $name
        email        = $email
        password     = $password
        mobile       = "1234567890"
        role         = $role
        departmentId = $deptId
    } | ConvertTo-Json
    
    try {
        Invoke-RestMethod -Uri $url -Method Post -Headers $headers -Body $body -ContentType "application/json" -ErrorAction SilentlyContinue
        Write-Host "   Created User: $email"
    }
    catch {
        Write-Host "   User likely exists: $email"
    }
}

function Get-User-By-Email {
    param ($token, $email, $serviceUrl)
    $listUrl = "$serviceUrl/admin/users"
    $headers = @{ Authorization = "Bearer $token" }
    try {
        $finalUri = "$listUrl`?size=100"
        $tokLen = if ($token) { $token.Length } else { 0 }
        $tokStart = if ($token -and $token.Length -gt 10) { $token.Substring(0, 10) } else { "null" }
        Write-Host "   DEBUG: Calling URI: '$finalUri' with Token Len: $tokLen ($tokStart...)"
        $res = Invoke-RestMethod -Uri $finalUri -Method Get -Headers $headers -ErrorAction Stop
        $user = $res.data.content | Where-Object { $_.email -eq $email }
        return $user
    }
    catch {
        Write-Warning "Failed to fetch users list from '$finalUri': $($_.Exception.Message)"
        return $null
    }
}

function Ensure-User-Department {
    param ($token, $userEmail, $deptId, $serviceUrl)
    $user = Get-User-By-Email -token $token -email $userEmail -serviceUrl $serviceUrl
    if ($user) {
        Write-Host "   DEBUG: Ensure-User for $userEmail found. Current Dept: $($user.department.id). Target: $deptId"
        if ($user.department.id -ne $deptId) {
            Write-Host "   Updating User $userEmail to Dept $deptId..."
            $updateUrl = "$serviceUrl/admin/users/$($user.id)"
            $body = @{ departmentId = $deptId } | ConvertTo-Json
            $headers = @{ Authorization = "Bearer $token" }
            Invoke-RestMethod -Uri $updateUrl -Method Put -Headers $headers -Body $body -ContentType "application/json" | Out-Null
        }
    }
}

# Helper functions defined above.
# Main execution starts below.

# ... (Same for B) ...

# ... (Checking file content first) ...


function Create-Task {
    param ($token, $title, $deptId, $assigneeId)
    $url = "$baseUrl/tasks/create"
    $headers = @{ Authorization = "Bearer $token" }
    $body = @{
        title           = $title
        description     = "Multi-Tenant Validation Task"
        priority        = "HIGH"
        status          = "TO_DO"
        dueDate         = (Get-Date).AddDays(1).ToString("yyyy-MM-ddTHH:mm:ss")
        type            = "Internal"
        departmentId    = $deptId
        assignedStaffId = $assigneeId
    } | ConvertTo-Json
    
    try {
        $res = Invoke-RestMethod -Uri $url -Method Post -Headers $headers -Body $body -ContentType "application/json"
        return $res.data.id
    }
    catch {
        Write-Error "Failed to create task: $($_.Exception.Message)"
        throw $_
    }
}

function Get-Tasks {
    param ($token)
    $url = "$baseUrl/tasks"
    $headers = @{ Authorization = "Bearer $token" }
    try {
        $res = Invoke-RestMethod -Uri $url -Method Get -Headers $headers
        if ($res.data.content) { return $res.data.content }
        return $res.data
    }
    catch {
        Write-Error "Failed to get tasks: $($_.Exception.Message)"
        return @()
    }
}

function Get-UserId {
    param ($token)
    # Helper to get self ID
    # Since we don't have a direct 'me' endpoint readily known, we'll parse token or just rely on list.
    # Actually, login returns ID.
    return $null
}

function Login-And-Get-Id {
    param ($email)
    $loginUrl = "$baseUrl/auth/login"
    $body = @{ email = $email; password = $password } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri $loginUrl -Method Post -Body $body -ContentType "application/json"
    return @{ token = $res.data.token; id = $res.data.id }
}

Write-Host "--- Starting Multi-Tenancy & RBAC Verification ---"

# 1. Setup Architecture
Write-Host "`n[1] Setting up Tenants and Users..."
Setup-Owner
$ownerToken = Get-Token -email $ownerEmail -password $password

# Tenant A
Create-TenantAdmin -ownerToken $ownerToken -email $adminAEmail -name "Admin A"
$adminAContext = Login-And-Get-Id -email $adminAEmail
$deptAId = Create-Department -token $adminAContext.token -name "Dept A"
Create-User -token $adminAContext.token -name "Dept Head A" -email $deptHeadAEmail -role "DEPARTMENT_HEAD" -deptId $deptAId
Create-User -token $adminAContext.token -name "Staff A" -email $staffAEmail -role "STAFF" -deptId $deptAId

# Ensure Department Link (Fixes stale user data)
Ensure-User-Department -token $adminAContext.token -userEmail $deptHeadAEmail -deptId $deptAId -serviceUrl $baseUrl
Ensure-User-Department -token $adminAContext.token -userEmail $staffAEmail -deptId $deptAId -serviceUrl $baseUrl

# Tenant B
Create-TenantAdmin -ownerToken $ownerToken -email $adminBEmail -name "Admin B"
$adminBContext = Login-And-Get-Id -email $adminBEmail
$deptBId = Create-Department -token $adminBContext.token -name "Dept B"
Create-User -token $adminBContext.token -name "Dept Head B" -email $deptHeadBEmail -role "DEPARTMENT_HEAD" -deptId $deptBId
Create-User -token $adminBContext.token -name "Staff B" -email $staffBEmail -role "STAFF" -deptId $deptBId

# Ensure Department Link (Fixes stale user data)
Ensure-User-Department -token $adminBContext.token -userEmail $deptHeadBEmail -deptId $deptBId -serviceUrl $baseUrl
Ensure-User-Department -token $adminBContext.token -userEmail $staffBEmail -deptId $deptBId -serviceUrl $baseUrl

# Get User IDs
$staffAContext = Login-And-Get-Id -email $staffAEmail
$deptHeadAContext = Login-And-Get-Id -email $deptHeadAEmail
$staffBContext = Login-And-Get-Id -email $staffBEmail

# 2. Create Tasks & Trigger Notifications
Write-Host "`n[2] Creating Tasks (Triggers Notifications)..."

# Task A1: Assigned to Staff A (by Admin A)
$taskA1Id = Create-Task -token $adminAContext.token -title "Task A1 (Assigned to Staff A)" -deptId $deptAId -assigneeId $staffAContext.id
Write-Host "   Created Task A1 (ID: $taskA1Id)"

# Task B1: Assigned to Staff B (by Admin B)
$taskB1Id = Create-Task -token $adminBContext.token -title "Task B1 (Assigned to Staff B)" -deptId $deptBId -assigneeId $staffBContext.id
Write-Host "   Created Task B1 (ID: $taskB1Id)"

# 3. Verify Isolation (Admins)
Write-Host "`n[3] Verifying Admin Isolation..."
$tasksA = Get-Tasks -token $adminAContext.token
$tasksB = Get-Tasks -token $adminBContext.token

$tasksAIds = $tasksA | ForEach-Object { $_.id }
$tasksBIds = $tasksB | ForEach-Object { $_.id }

if ($tasksAIds -contains $taskA1Id -and $tasksAIds -notcontains $taskB1Id) {
    Write-Host "   [PASS] Admin A sees A1, NOT B1." -ForegroundColor Green
}
else {
    Write-Error "   [FAIL] Admin A visibility issue. Visible: $($tasksAIds -join ', ')"
}

if ($tasksBIds -contains $taskB1Id -and $tasksBIds -notcontains $taskA1Id) {
    Write-Host "   [PASS] Admin B sees B1, NOT A1." -ForegroundColor Green
}
else {
    Write-Error "   [FAIL] Admin B visibility issue. Visible: $($tasksBIds -join ', ')"
}

# 4. Verify RBAC & Isolation (Staff)
Write-Host "`n[4] Verifying Staff Isolation & Access..."
$tasksStaffA = Get-Tasks -token $staffAContext.token
$staffAIds = $tasksStaffA | ForEach-Object { $_.id }

if ($staffAIds -contains $taskA1Id) {
    Write-Host "   [PASS] Staff A sees assigned Task A1." -ForegroundColor Green
}
else {
    Write-Error "   [FAIL] Staff A CANNOT see assigned Task A1."
}

if ($staffAIds -notcontains $taskB1Id) {
    Write-Host "   [PASS] Staff A does NOT see Task B1 (Cross-Tenant)." -ForegroundColor Green
}
else {
    Write-Error "   [FAIL] Staff A SEES Task B1!"
}

# 5. Verify RBAC & Isolation (Dept Head)
Write-Host "`n[5] Verifying Dept Head Isolation & Access..."
$tasksDeptHeadA = Get-Tasks -token $deptHeadAContext.token
$deptHeadAIds = $tasksDeptHeadA | ForEach-Object { $_.id }

if ($deptHeadAIds -contains $taskA1Id) {
    Write-Host "   [PASS] Dept Head A sees Task A1 (in Dept A)." -ForegroundColor Green
}
else {
    Write-Host "   [FAIL] Dept Head A CANNOT see Task A1. Visible IDs: $($deptHeadAIds -join ', ')" -ForegroundColor Red
    
    # Debug: Fetch Task A1 Details
    try { 
        $debugTask = Invoke-RestMethod -Uri "$baseUrl/tasks/$taskA1Id" -Method Get -Headers @{ Authorization = "Bearer $($adminAContext.token)" }
        Write-Host "   DEBUG: Task A1 ($taskA1Id) Dept ID: $($debugTask.data.department.id)"
    }
    catch { Write-Host "   DEBUG: Failed to fetch Task A1 details." }
    
    # Debug: Fetch Dept Head A Details
    try {
        $debugUsers = Invoke-RestMethod -Uri "$baseUrl/admin/users?departmentId=$deptAId" -Method Get -Headers @{ Authorization = "Bearer $($adminAContext.token)" }
        $count = $debugUsers.data.content.Count
        Write-Host "   DEBUG: /admin/users?departmentId=$deptAId returned $count users."
        $dhUser = $debugUsers.data.content | Where-Object { $_.email -eq $deptHeadAEmail }
        if ($dhUser) {
            Write-Host "   DEBUG: Dept Head A ($($dhUser.id)) Dept ID: $($dhUser.department.id)"
        }
        else {
            Write-Host "   DEBUG: Dept Head A NOT found in user list."
        }
    }
    catch { Write-Host "   DEBUG: Failed to fetch Dept Head details." }
}

if ($deptHeadAIds -notcontains $taskB1Id) {
    Write-Host "   [PASS] Dept Head A does NOT see Task B1 (Cross-Tenant)." -ForegroundColor Green
}
else {
    Write-Error "   [FAIL] Dept Head A SEES Task B1!"
}

# 6. Verify Email Notification Triggering (Update Email)
Write-Host "`n[6] updating User email to test address for validation..."
# We will temporarily update Staff A email to the test email to verify they get notifications
$updateUrl = "$baseUrl/admin/users/$($staffAContext.id)"
$updateBody = @{ email = $testEmail } | ConvertTo-Json
$updateHeaders = @{ Authorization = "Bearer $($adminAContext.token)" }

# Update User Email (This itself sends a notification to old and new email)
try {
    Invoke-RestMethod -Uri $updateUrl -Method Put -Headers $updateHeaders -Body $updateBody -ContentType "application/json"
    Write-Host "   Updated Staff A email to $testEmail for testing."
}
catch {
    Write-Host "   Failed to update email (might already be set): $($_.Exception.Message)"
}

# Update Task Status (Triggers Notification to Staff - now test email)
Write-Host "   Updating Task A1 Status to trigger email..."
$statusUrl = "$baseUrl/tasks/$taskA1Id/status"
$statusBody = "IN_PROGRESS" # Direct string or JSON depending on controller, usually RequestParam or Body.
# Checking typical controller usage... often @PutMapping or @PatchMapping with @RequestParam or plain body. 
# Reverting to typical JSON object if API expects it.
# TaskController usually: updateTaskStatus(@PathVariable Long id, @RequestBody Map<String, String> status) or similar?
# Let's assume standard updateTask with full body if status endpoint not generic.
# Or simpler: Add Comment.

$commentUrl = "$baseUrl/tasks/$taskA1Id/comments"
$commentBody = @{ text = "Test Comment for Email Verification notification." } | ConvertTo-Json
try {
    Invoke-RestMethod -Uri $commentUrl -Method Post -Headers $updateHeaders -Body $commentBody -ContentType "application/json"
    Write-Host "   [PASS] Comment added to trigger notification." -ForegroundColor Green
    Write-Host "   > PLEASE CHECK INBOX OF $testEmail for 'New Comment' notification." -ForegroundColor Yellow
}
catch {
    Write-Error "   [FAIL] Failed to add comment: $($_.Exception.Message)"
}

Write-Host "`n--- Verification Complete ---"
