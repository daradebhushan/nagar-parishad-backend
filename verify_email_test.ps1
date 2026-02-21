$ErrorActionPreference = "Stop"

# Configuration
$baseUrl = "http://localhost:8080/api"
$ownerEmail = "owner_sys_iso@test.com" # Existing Owner
$testEmail = "daradebhushan15@gmail.com"
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

function Create-TenantAdmin {
    param ($ownerToken, $email, $name)
    $url = "$baseUrl/owner/create-admin"
    $headers = @{ Authorization = "Bearer $ownerToken" }
    
    $body = @{ name = $name; email = $email; password = $password; mobile = "1234567890"; role = "ADMIN" } | ConvertTo-Json
    
    try {
        Invoke-RestMethod -Uri $url -Method Post -Headers $headers -Body $body -ContentType "application/json" -ErrorAction SilentlyContinue
        Write-Host "   Created Test Admin: $email"
    }
    catch {
        Write-Host "   Test Admin likely exists: $email ($($_.Exception.Message))"
    }
}

function Create-Task {
    param ($token, $title)
    $url = "$baseUrl/tasks/create"
    $headers = @{ Authorization = "Bearer $token" }
    $body = @{
        title       = $title
        description = "Email Verification Task"
        priority    = "HIGH"
        status      = "TO_DO"
        dueDate     = (Get-Date).AddDays(1).ToString("s")
    } | ConvertTo-Json
    
    $res = Invoke-RestMethod -Uri $url -Method Post -Headers $headers -Body $body -ContentType "application/json"
    Write-Host "   Task Created: $($res.data.id)"
    return $res.data.id
}

function Update-Task-Status {
    param ($token, $taskId, $status)
    $url = "$baseUrl/tasks/$taskId/status?status=$status"
    $headers = @{ Authorization = "Bearer $token" }
    
    Invoke-RestMethod -Uri $url -Method Put -Headers $headers -ContentType "application/json"
    Write-Host "   Task Status Updated to $status"
}

Write-Host "--- Starting Email Verification ---"

# 1. Login Owner
Write-Host "1. Logging in as Owner..."
# Assuming Owner setup from previous step
$ownerToken = Get-Token -email $ownerEmail -password $password

# 2. Create Test Admin
Write-Host "2. Creating Test Admin ($testEmail)..."

# Try to find and delete existing user first
try {
    $searchHeaders = @{ Authorization = "Bearer $ownerToken" }
    $users = Invoke-RestMethod -Uri "$baseUrl/admin/users" -Headers $searchHeaders -Method Get -ErrorAction SilentlyContinue
    $existingUser = $users.data.content | Where-Object { $_.email -eq $testEmail }
    
    if ($existingUser) {
        Write-Host "   Found existing user (ID: $($existingUser.id)). Updating Password..."
        $updateBody = @{ password = $password } | ConvertTo-Json
        try {
            Invoke-RestMethod -Uri "$baseUrl/admin/users/$($existingUser.id)" -Method Put -Headers $searchHeaders -Body $updateBody -ContentType "application/json"
            Write-Host "   Password Updated."
            # Skip creation
        }
        catch {
            Write-Host "   Password Update Failed: $_"
            # Continue to try creation? No, likely fail.
        }
    }
    else {
        Create-TenantAdmin -ownerToken $ownerToken -email $testEmail -name "Test Admin B D"
    }
}
catch {
    Write-Host "   Error checking/updating existing user: $_"
}


# 3. Login Test Admin
Write-Host "3. Logging in as Test Admin..."
$testToken = Get-Token -email $testEmail -password $password

# 4. Trigger Notifications
Write-Host "4. Triggering Notifications..."
# 4a. Task Creation with Notification (In-App)
$taskId = Create-Task -token $testToken -title "Email Test Task"

# 4b. Status Change (Should trigger Email if logic allows self-notification or we need another user)
# Requirement: "When admin assigns a task... Send notification to... Admin"
# NotificationService skips self? "if (u1.getid equals u2.getid) return" - YES.
# So if I create task myself, I don't get notification.
# I need TWO users to test email to Admin.
# Test Admin -> Create Staff -> Admin assigns task to Staff -> Staff gets email?
# Or Staff updates task -> Admin gets email.

# Let's create a Staff under Test Admin.
$staffEmail = "daradebhushan15+staff@gmail.com"
Write-Host "   Creating Staff ($staffEmail)..."
$staffUrl = "$baseUrl/admin/users"
$staffBody = @{ name = "Test Staff"; email = $staffEmail; password = $password; mobile = "1234567890"; role = "STAFF"; departmentId = 1; designationId = 1 } | ConvertTo-Json
# Need Dept/Designation ID. Assuming 1 exists or I need to create.
# I'll try to fetch Departments first.
$deptUrl = "$baseUrl/departments"
$headersCheck = @{ Authorization = "Bearer $testToken" }
$depts = Invoke-RestMethod -Uri $deptUrl -Method Get -Headers $headersCheck
$deptId = $depts.data[0].id

if (-not $deptId) {
    # Create Department
    Write-Host "   Creating Department..."
    $createDeptUrl = "$baseUrl/departments"
    $deptBody = @{ name = "Test Dept" } | ConvertTo-Json
    $dRes = Invoke-RestMethod -Uri $createDeptUrl -Method Post -Headers $headersCheck -Body $deptBody -ContentType "application/json"
    $deptId = $dRes.data.id
}

# Designation
$desigUrl = "$baseUrl/designations"
$desigs = Invoke-RestMethod -Uri $desigUrl -Method Get -Headers $headersCheck
$desigId = $desigs.data[0].id
if (-not $desigId) {
    $createDesigUrl = "$baseUrl/designations"
    $desigBody = @{ name = "Test Designation" } | ConvertTo-Json
    $desRes = Invoke-RestMethod -Uri $createDesigUrl -Method Post -Headers $headersCheck -Body $desigBody -ContentType "application/json"
    $desigId = $desRes.data.id
}

$staffBody = @{ name = "Test Staff"; email = $staffEmail; password = $password; mobile = "1234567890"; role = "STAFF"; departmentId = $deptId; designationId = $desigId } | ConvertTo-Json

try {
    Invoke-RestMethod -Uri $staffUrl -Method Post -Headers $headersCheck -Body $staffBody -ContentType "application/json" -ErrorAction SilentlyContinue
    Write-Host "   Staff Created."
}
catch {
    Write-Host "   Staff exists."
}

$staffToken = Get-Token -email $staffEmail -password $password

# NOW: Staff updates Task -> Admin ($testEmail) should get Email.
# First Admin assigns task to Staff.
Write-Host "   Assigning Task to Staff..."
$assignUrl = "$baseUrl/tasks/$taskId"
$assignBody = @{ title = "Updated Test Task"; assignedStaffId = (Invoke-RestMethod -Uri "$baseUrl/admin/users?email=$staffEmail" -Headers $headersCheck -Method Get).data.content[0].id; priority = "HIGH"; status = "TO_DO"; dueDate = "2026-01-01T00:00:00" } | ConvertTo-Json 
# Need Full Update Body? TaskRequest?
# Service.updateTask requires TaskRequest.
# Let's just create NEW task assigned to Staff.
$newTaskBody = @{
    title           = "Task for Staff Notification"
    description     = "Test Description"
    priority        = "HIGH"
    status          = "TO_DO"
    dueDate         = (Get-Date).AddDays(1).ToString("yyyy-MM-ddTHH:mm:ss")
    assignedStaffId = (Invoke-RestMethod -Uri "$baseUrl/admin/users" -Headers $headersCheck -Method Get).data.content | Where-Object { $_.email -eq $staffEmail } | Select-Object -ExpandProperty id
    departmentId    = $deptId
} | ConvertTo-Json

$newTaskRes = Invoke-RestMethod -Uri "$baseUrl/tasks/create" -Method Post -Headers $headersCheck -Body $newTaskBody -ContentType "application/json"
$newTaskId = $newTaskRes.data.id
Write-Host "   Created Task $newTaskId assigned to Staff."
Write-Host "   -> Staff ($staffEmail) should receive Task Assignment Email."

# Staff updates status -> Admin ($testEmail) receives Email
Write-Host "   Staff updating status..."
$statusUrl = "$baseUrl/tasks/$newTaskId/status?status=IN_PROGRESS"
$staffHeaders = @{ Authorization = "Bearer $staffToken" }
Invoke-RestMethod -Uri $statusUrl -Method Put -Headers $staffHeaders

Write-Host "   -> Admin ($testEmail) should receive Status Update Email."
Write-Host "--- Email Verification Actions Completed ---"
