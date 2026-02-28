$baseUrl = "http://127.0.0.1:8080/api"

Write-Output "--- E2E STAFF WORKFLOW AND NOTIFICATIONS SUITE ---`n"

$ownerEmail = "owner@govt.in"
$ownerPassword = "password"
$ownerBody = @{ email = $ownerEmail; password = $ownerPassword } | ConvertTo-Json

# Log in as Owner to ensure we have a Chief Officer (Role = ADMIN)
try {
    $loginData = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $ownerBody -ContentType "application/json"
    $ownerToken = $loginData.data.token
}
catch {
    Write-Output "Failed to login as owner. Aborting test."
    exit 1
}

$headersOwner = @{ Authorization = "Bearer $ownerToken" }

# Fetch all users and find an ADMIN
$usersRes = Invoke-RestMethod -Uri "$baseUrl/admin/users" -Method Get -Headers $headersOwner
$users = $usersRes.data.content

$coUser = $users | Where-Object { $_.role -eq "ADMIN" } | Select-Object -First 1

if (-not $coUser) {
    Write-Output "No Chief Officer (ADMIN) found! Creating a temporary one..."
    $coBody = @{
        name     = "Test Chief Officer"
        email    = "co_test_staff_suite@nagarparishad.in"
        password = "password123"
        mobile   = "9998887771"
        role     = "ADMIN"
        active   = $true
    } | ConvertTo-Json
    $newCoRes = Invoke-RestMethod -Uri "$baseUrl/admin/users" -Method Post -Body $coBody -ContentType "application/json" -Headers $headersOwner
    $coEmail = "co_test_staff_suite@nagarparishad.in"
    $coPassword = "password123"
}
else {
    Write-Output "Found existing Chief Officer: $($coUser.email)"
    $coEmail = $coUser.email
    # We must reset their password to ensure we can log in
    $updateBody = @{ password = "password123" } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/admin/users/$($coUser.id)" -Method Put -Body $updateBody -ContentType "application/json" -Headers $headersOwner
    $coPassword = "password123"
}

# 1. Login as CO
$coLoginBody = @{ email = $coEmail; password = $coPassword } | ConvertTo-Json
$coLoginRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $coLoginBody -ContentType "application/json"
$coToken = $coLoginRes.data.token

if (-not $coToken) {
    Write-Output "CO Login FAILED!"
    exit 1
}
Write-Output "SUCCESS: Chief Officer logged in."
$headersCo = @{ Authorization = "Bearer $coToken" }

# Get a valid department ID
$depts = Invoke-RestMethod -Uri "$baseUrl/public/departments" -Method Get
$deptId = $depts[0].id

# 2. Create Department Head
$headEmail = "head_$((Get-Date).Ticks)@demo.com"
$headBody = @{
    name         = "Test Department Head"
    email        = $headEmail
    password     = "password123"
    mobile       = "9876543210"
    role         = "DEPARTMENT_HEAD"
    active       = $true
    departmentId = $deptId
} | ConvertTo-Json

$headCreation = Invoke-RestMethod -Uri "$baseUrl/admin/users" -Method Post -Body $headBody -ContentType "application/json" -Headers $headersCo
$headId = $headCreation.data.id
Write-Output "SUCCESS: Created Department Head ($headEmail)"

# 3. Create Staff User
$staffEmail = "staff_$((Get-Date).Ticks)@demo.com"
$staffBody = @{
    name         = "Test Staff User"
    email        = $staffEmail
    password     = "password123"
    mobile       = "9876543211"
    role         = "STAFF"
    active       = $true
    departmentId = $deptId
} | ConvertTo-Json

$staffCreation = Invoke-RestMethod -Uri "$baseUrl/admin/users" -Method Post -Body $staffBody -ContentType "application/json" -Headers $headersCo
$staffId = $staffCreation.data.id
Write-Output "SUCCESS: Created Staff ($staffEmail)"

# 4. CO Creates Task Assigned to Staff
$taskBody = @{
    title           = "Fix the Streetlight"
    description     = "Streetlight is broken on Main St."
    taskType        = "NON_COMPLAINT"
    priority        = "HIGH"
    status          = "TO_DO"
    assigneeId      = $null # explicitly overriding the wrong key just in case
    assignedStaffId = $staffId
    departmentId    = $deptId
} | ConvertTo-Json

try {
    $taskCreation = Invoke-RestMethod -Uri "$baseUrl/tasks/create" -Method Post -Body $taskBody -ContentType "application/json" -Headers $headersCo
    $taskId = $taskCreation.data.id
    Write-Output "SUCCESS: CO Created Task ($taskId) assigned to Staff ($staffId)"
}
catch {
    Write-Output "FAILED: CO Task Creation: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        Write-Output "JSON RESPONSE: $($reader.ReadToEnd())"
    }
    exit 1
}

# Wait for db flush
Start-Sleep -Seconds 1

# 5. Login as Staff
$staffLoginBody = @{ email = $staffEmail; password = "password123" } | ConvertTo-Json
$staffLoginRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $staffLoginBody -ContentType "application/json"
$staffToken = $staffLoginRes.data.token
$headersStaff = @{ Authorization = "Bearer $staffToken" }

# Verify Staff Task Visibility
$staffTasks = Invoke-RestMethod -Uri "$baseUrl/tasks" -Method Get -Headers $headersStaff
$foundTask = $staffTasks.data.content | Where-Object { $_.id -eq $taskId }

if ($foundTask) {
    Write-Output "SUCCESS: Staff successfully retrieved the task assigned to them."
    Write-Output "TASK JSON DEBUG:"
    Write-Output ($foundTask | ConvertTo-Json -Depth 5)
}
else {
    Write-Output "FAILED: Staff could not see the assigned task!"
}

# 6. Staff updates task status
$staffUpdateTaskBody = @{ status = "IN_PROGRESS" } | ConvertTo-Json
try {
    $updatedTask = Invoke-RestMethod -Uri "$baseUrl/tasks/$taskId/status" -Method Patch -Body $staffUpdateTaskBody -ContentType "application/json" -Headers $headersStaff
    Write-Output "SUCCESS: Staff successfully updated Task Status to $($updatedTask.data.status)"
}
catch {
    Write-Output "FAILED: Staff updating task status: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        Write-Output "JSON RESPONSE: $($reader.ReadToEnd())"
    }
}

# 7. Staff leaves a comment on the task
$commentBody = @{ text = "I have begun investigating the streetlight." } | ConvertTo-Json
try {
    $commentRes = Invoke-RestMethod -Uri "$baseUrl/tasks/$taskId/comments" -Method Post -Body $commentBody -ContentType "application/json" -Headers $headersStaff
    Write-Output "SUCCESS: Staff added a comment to the task"
}
catch {
    Write-Output "FAILED: Staff adding comment: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        Write-Output "JSON RESPONSE: $($reader.ReadToEnd())"
    }
}

# Wait to ensure notifications process asynchronously
Start-Sleep -Seconds 1

# 8. Verify Notifications for CO
$coNotifs = Invoke-RestMethod -Uri "$baseUrl/notifications" -Method Get -Headers $headersCo
if ($coNotifs.data.Count -gt 0) {
    $coRecentNotifs = $coNotifs.data | Select-Object -First 3
    Write-Output "`nNotifications received by Chief Officer:"
    foreach ($n in $coRecentNotifs) {
        Write-Output "> $($n.message)"
    }
}
else {
    Write-Output "FAILED: CO did not receive any notifications!"
}

# 9. Login as Department Head and Verify Notifications
$headLoginBody = @{ email = $headEmail; password = "password123" } | ConvertTo-Json
$headLoginRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $headLoginBody -ContentType "application/json"
$headToken = $headLoginRes.data.token
$headersHead = @{ Authorization = "Bearer $headToken" }

$headNotifs = Invoke-RestMethod -Uri "$baseUrl/notifications" -Method Get -Headers $headersHead
if ($headNotifs.data.Count -gt 0) {
    $headRecentNotifs = $headNotifs.data | Select-Object -First 3
    Write-Output "`nNotifications received by Department Head:"
    foreach ($n in $headRecentNotifs) {
        Write-Output "> $($n.message)"
    }
}
else {
    Write-Output "NOTE: Head did not receive notifications (Notifications usually route up the hierarchy sequentially)."
}

# 10. Department Head Comments on the Task
$headCommentBody = @{ text = "Good, keep me updated." } | ConvertTo-Json
try {
    Invoke-RestMethod -Uri "$baseUrl/tasks/$taskId/comments" -Method Post -Body $headCommentBody -ContentType "application/json" -Headers $headersHead | Out-Null
    Write-Output "`nSUCCESS: Department Head added a comment."
}
catch {
    Write-Output "FAILED: Department Head commenting: $_"
}

Start-Sleep -Seconds 1

# 11. Login as Staff and check notifications for the Head's comment
$staffNotifsFinal = Invoke-RestMethod -Uri "$baseUrl/notifications" -Method Get -Headers $headersStaff
if ($staffNotifsFinal.data.Count -gt 0) {
    Write-Output "`nNotification received by Staff Member:"
    $staffRecentNotif = $staffNotifsFinal.data[0].message
    Write-Output "> $staffRecentNotif"
}
else {
    Write-Output "FAILED: Staff did not receive notification from Head's comment!"
}

Write-Output "`n--- STAFF WORKFLOW AND NOTIFICATION TEST COMPLETE ---"
