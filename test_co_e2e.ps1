$baseUrl = "http://127.0.0.1:8080/api"

Write-Host "--- E2E CHIEF OFFICER (ADMIN) API TEST SUITE ---`n"

$ownerEmail = "owner@govt.in"
$ownerPassword = "password"
$ownerBody = @{ email = $ownerEmail; password = $ownerPassword } | ConvertTo-Json

# Log in as Owner to ensure we have a Chief Officer (Role = ADMIN)
try {
    $loginRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $ownerBody -ContentType "application/json"
    $ownerToken = $loginRes.data.token
}
catch {
    Write-Host "Failed to login as owner. Aborting test."
    exit 1
}

$headersOwner = @{ Authorization = "Bearer $ownerToken" }

# Fetch all users and find an ADMIN
$usersRes = Invoke-RestMethod -Uri "$baseUrl/admin/users" -Method Get -Headers $headersOwner
$users = $usersRes.data.content

$coUser = $users | Where-Object { $_.role -eq "ADMIN" } | Select-Object -First 1

if (-not $coUser) {
    Write-Host "No Chief Officer (ADMIN) found! Creating a temporary one..."
    $coBody = @{
        name     = "Test Chief Officer"
        email    = "co_test_final@nagarparishad.in"
        password = "password123"
        mobile   = "9998887771"
        role     = "ADMIN"
        active   = $true
    } | ConvertTo-Json
    
    try {
        $newCoRes = Invoke-RestMethod -Uri "$baseUrl/admin/users" -Method Post -Body $coBody -ContentType "application/json" -Headers $headersOwner
        $coEmail = "co_test_final@nagarparishad.in"
        $coPassword = "password123"
        Write-Host "Created new Temporary Chief Officer ($coEmail)"
    }
    catch {
        Write-Host "Failed to create CO:"
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        Write-Host "JSON RESPONSE: $($reader.ReadToEnd())"
        exit 1
    }
}
else {
    Write-Host "Found existing Chief Officer: $($coUser.email)"
    $coEmail = $coUser.email
    # We must reset their password to ensure we can log in
    $updateBody = @{ password = "password123" } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/admin/users/$($coUser.id)" -Method Put -Body $updateBody -ContentType "application/json" -Headers $headersOwner
    $coPassword = "password123"
}

Write-Host "`n>> 1. CO LOGIN TEST <<"
$coLoginBody = @{ email = $coEmail; password = $coPassword } | ConvertTo-Json
$coLoginRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $coLoginBody -ContentType "application/json"
$coToken = $coLoginRes.data.token

if (-not $coToken) {
    Write-Host "CO Login FAILED!"
    exit 1
}
Write-Host "SUCCESS: CO logged in. Roles: $($coLoginRes.data.roles -join ', ')"
$headersCo = @{ Authorization = "Bearer $coToken" }

Write-Host "`n>> 2. CO DASHBOARD / NOTIFICATIONS TEST <<"
try {
    $stats = Invoke-RestMethod -Uri "$baseUrl/dashboard/stats" -Method Get -Headers $headersCo
    Write-Host "SUCCESS: Fetched Dashboard Stats! Complaints: $($stats.data.totalComplaints), Tasks: $($stats.data.totalTasks)"

    $notifs = Invoke-RestMethod -Uri "$baseUrl/notifications" -Method Get -Headers $headersCo
    Write-Host "SUCCESS: Fetched Notifications. Count: $($notifs.data.content.Count)"
}
catch {
    Write-Host "FAILED to fetch CO dashboards/notifications: $_"
}

Write-Host "`n>> 3. CO CRUD DEPARTMENT TEST <<"
try {
    $deptBody = @{ name = "CO API Test Dept Final"; chatbotMarathiLabel = "CO Dept Final" } | ConvertTo-Json
    $newDept = Invoke-RestMethod -Uri "$baseUrl/admin/departments" -Method Post -Body $deptBody -ContentType "application/json" -Headers $headersCo
    Write-Host "SUCCESS: CO Created Department (ID: $($newDept.data.id))"

    $deptUpdateBody = @{ name = "CO API Test Dept Final V2" } | ConvertTo-Json
    $updatedDept = Invoke-RestMethod -Uri "$baseUrl/admin/departments/$($newDept.data.id)" -Method Put -Body $deptUpdateBody -ContentType "application/json" -Headers $headersCo
    Write-Host "SUCCESS: CO Updated Department Name to '$($updatedDept.data.name)'"

    Invoke-RestMethod -Uri "$baseUrl/admin/departments/$($newDept.data.id)" -Method Delete -Headers $headersCo
    Write-Host "SUCCESS: CO Deleted Department"
}
catch {
    Write-Host "FAILED on Dept CRUD: $_"
}

Write-Host "`n>> 4. CO CRUD STAFF/USER TEST <<"
try {
    # We need a valid department to assign staff to
    $depts = Invoke-RestMethod -Uri "$baseUrl/public/departments" -Method Get
    $deptId = $depts.data[0].id

    $staffBody = @{
        name         = "CO Staff API Test Final"
        email        = "staff_api_final_$((Get-Date).Ticks)@demo.com"
        password     = "password123"
        mobile       = "8877665555"
        role         = "STAFF"
        active       = $true
        departmentId = $deptId
    } | ConvertTo-Json

    $newStaff = Invoke-RestMethod -Uri "$baseUrl/admin/users" -Method Post -Body $staffBody -ContentType "application/json" -Headers $headersCo
    Write-Host "SUCCESS: CO Created Staff User (ID: $($newStaff.data.id))"

    $staffUpdateBody = @{ name = "CO Staff API Test Final Updated"; active = $false } | ConvertTo-Json
    $updatedStaff = Invoke-RestMethod -Uri "$baseUrl/admin/users/$($newStaff.data.id)" -Method Put -Body $staffUpdateBody -ContentType "application/json" -Headers $headersCo
    Write-Host "SUCCESS: CO Updated Staff (Active: $($updatedStaff.data.active))"

    # Try Delete
    try {
        Invoke-RestMethod -Uri "$baseUrl/admin/users/$($newStaff.data.id)" -Method Delete -Headers $headersCo
        Write-Host "SUCCESS: CO Deleted Staff User"
    }
    catch {
        Write-Host "NOTE: CO couldn't cleanly delete staff (likely constraint or logic): $_"
    }
}
catch {
    Write-Host "FAILED on Staff CRUD: $_"
}

Write-Host "`n>> 5. CO CRUD TASKS TEST <<"
$taskBody = @{
    title       = "CO E2E Task Final"
    description = "Pothole repair test via API Final"
    taskType    = "NON_COMPLAINT"
    priority    = "HIGH"
    status      = "TO_DO"
} | ConvertTo-Json

try {
    $newTask = Invoke-RestMethod -Uri "$baseUrl/tasks" -Method Post -Body $taskBody -ContentType "application/json" -Headers $headersCo
    Write-Host "SUCCESS: CO Created Task (ID: $($newTask.data.id))"

    $updateTaskBody = @{ status = "IN_PROGRESS"; priority = "URGENT" } | ConvertTo-Json
    $updatedTask = Invoke-RestMethod -Uri "$baseUrl/tasks/$($newTask.data.id)" -Method Put -Body $updateTaskBody -ContentType "application/json" -Headers $headersCo
    Write-Host "SUCCESS: CO Updated Task Status to '$($updatedTask.data.status)'"

    $tasks = Invoke-RestMethod -Uri "$baseUrl/tasks" -Method Get -Headers $headersCo
    Write-Host "SUCCESS: CO Fetched Tasks. Found $($tasks.data.content.Count) tasks."
}
catch {
    Write-Host "FAILED on Task CRUD: $_"
}

Write-Host "`n>> 6. CO COMPLAINTS MANAGEMENT TEST <<"
try {
    $complaints = Invoke-RestMethod -Uri "$baseUrl/admin/complaints" -Method Get -Headers $headersCo
    Write-Host "SUCCESS: CO Fetched Complaints. Found $($complaints.data.content.Count) complaints globally."
    
    if ($complaints.data.content.Count -gt 0) {
        $compId = $complaints.data.content[0].id
        Write-Host "CO Attempting to fetch specific complaint $compId ..."
        $compSingle = Invoke-RestMethod -Uri "$baseUrl/admin/complaints/$compId" -Method Get -Headers $headersCo
        Write-Host "SUCCESS: CO Fetched Complaint details: $($compSingle.data.name)"
    }
}
catch {
    Write-Host "FAILED on Complaint Management: $_"
}

Write-Host "`n--- CO E2E TESTING COMPLETE ---"
