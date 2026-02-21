$baseUrl = "http://localhost:8080/api"

# 1. Login as Admin
$adminLogin = @{ email = "admin@test.com"; password = "password" } | ConvertTo-Json
$adminRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $adminLogin -ContentType "application/json"
$adminToken = $adminRes.data.token
$adminHeaders = @{ Authorization = "Bearer $adminToken" }
Write-Host "Admin Logged In."

# 2. Get Department and Staff for Task Creation (Need real IDs)
# Assuming Dept 1 and Staff exist. If not, this might fail, but let's try.
# We'll just list users to find a Staff and Dept.
$usersRes = Invoke-RestMethod -Uri "$baseUrl/admin/users" -Method Get -Headers $adminHeaders
$staffUser = $usersRes.data.content | Where-Object { $_.role -eq "STAFF" } | Select-Object -First 1

if (-not $staffUser) {
    Write-Error "No STAFF user found to test with."
    exit
}
Write-Host "Found Staff: $($staffUser.email) (ID: $($staffUser.id))"

$deptsRes = Invoke-RestMethod -Uri "$baseUrl/admin/departments" -Method Get -Headers $adminHeaders
$dept = $deptsRes.data.content | Select-Object -First 1
Write-Host "Found Dept: $($dept.name) (ID: $($dept.id))"

# 3. Create Task as Admin
$taskData = @{
    title           = "Test Task Permissions";
    description     = "Testing update restrictions";
    priority        = "MEDIUM";
    status          = "TO_DO";
    departmentId    = $dept.id;
    assignedStaffId = $staffUser.id;
    dueDate         = (Get-Date).AddDays(1).ToString("yyyy-MM-ddTHH:mm:ss")
} | ConvertTo-Json

$taskRes = Invoke-RestMethod -Uri "$baseUrl/tasks/create" -Method Post -Body $taskData -Headers $adminHeaders -ContentType "application/json"
$taskId = $taskRes.data.id
Write-Host "Task Created: ID $taskId"

# 4. Login as Staff
# We need the staff password. In init.sql it's usually 'password' or hashed.
# Assuming 'password' works for seeded users.
$staffLogin = @{ email = $staffUser.email; password = "password" } | ConvertTo-Json
try {
    $staffRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $staffLogin -ContentType "application/json"
    $staffToken = $staffRes.data.token
    $staffHeaders = @{ Authorization = "Bearer $staffToken" }
    Write-Host "Staff Logged In."
}
catch {
    Write-Error "Failed to login as staff. Password might not be 'password'. Skipping staff tests."
    exit
}

# 5. Try Update Task (PUT) - Should Fail (403)
Write-Host "Attempting Full Update as Staff (Expected: 403 Forbidden)..."
$updateData = @{
    title           = "Hacked Title";
    description     = "Hacked Desc";
    priority        = "HIGH";
    status          = "IN_PROGRESS";
    departmentId    = $dept.id;
    assignedStaffId = $staffUser.id
} | ConvertTo-Json

try {
    Invoke-RestMethod -Uri "$baseUrl/tasks/$taskId" -Method Put -Body $updateData -Headers $staffHeaders -ContentType "application/json"
    Write-Error "FAILURE: Staff was able to fully update task!"
}
catch {
    if ($_.Exception.Response.StatusCode -eq [System.Net.HttpStatusCode]::Forbidden) {
        Write-Host "SUCCESS: Full Update Forbidden (403)." -ForegroundColor Green
    }
    else {
        Write-Error "Unexpected Error: $($_.Exception.Message)"
    }
}

# 6. Try Update Status (PATCH) - Should Succeed (200)
Write-Host "Attempting Status Update as Staff (Expected: 200 OK)..."
$statusData = @{ status = "IN_PROGRESS" } | ConvertTo-Json

try {
    $patchRes = Invoke-RestMethod -Uri "$baseUrl/tasks/$taskId/status" -Method Patch -Body $statusData -Headers $staffHeaders -ContentType "application/json"
    if ($patchRes.data.status -eq "IN_PROGRESS") {
        Write-Host "SUCCESS: Status Updated to IN_PROGRESS." -ForegroundColor Green
    }
    else {
        Write-Error "Failed to update status. Response: $($patchRes | ConvertTo-Json -Depth 5)"
    }
}
catch {
    Write-Error "Update Status Failed: $($_.Exception.Message)"
    # Setup debug printing
    $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
    $responseBody = $reader.ReadToEnd()
    Write-Host "Response Body: $responseBody"
}
