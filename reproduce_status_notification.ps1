
# Script to reproduce Task Status Update and check for notifications

$baseUrl = "http://localhost:8080/api"
$staffLogin = @{
    email    = "daradebhushan15+staff@gmail.com"
    password = "password"
}

# 1. Login as Staff
$loginResponse = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body ($staffLogin | ConvertTo-Json) -ContentType "application/json"
$staffToken = $loginResponse.data.token
$staffHeaders = @{ Authorization = "Bearer $staffToken" }

Write-Host "Logged in as Staff: $($loginResponse.data.email)"

# 2. Try to find an existing task assigned to staff
try {
    $tasksResponse = Invoke-RestMethod -Uri "$baseUrl/tasks" -Method Get -Headers $staffHeaders
    $task = $tasksResponse.data.content | Select-Object -First 1
}
catch {
    Write-Host "Failed to list tasks: $_"
}

if (-not $task) {
    Write-Host "No tasks found for staff. Creating one..."
    # Login as Admin to create task
    $adminLogin = @{
        email    = "daradebhushan15+admin@gmail.com"
        password = "password"
    }
    $adminRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body ($adminLogin | ConvertTo-Json) -ContentType "application/json"
    $adminToken = $adminRes.data.token
    $adminHeaders = @{ Authorization = "Bearer $adminToken" }

    # Get Staff ID
    $staffId = $loginResponse.data.id

    # Get a department ID (needed for task)
    try {
        $deptRes = Invoke-RestMethod -Uri "$baseUrl/departments" -Method Get -Headers $adminHeaders
        $deptId = $deptRes.data.content[0].id
    }
    catch {
        Write-Host "Error fetching departments, using explicit ID 1 if possible"
        $deptId = 1
    }

    $newTask = @{
        title           = "Test Notification Task"
        description     = "Task to test status change notification"
        priority        = "MEDIUM"
        status          = "TO_DO"
        type            = "Internal"
        # Fix: Use full ISO 8601 format for LocalDateTime
        dueDate         = (Get-Date).AddDays(1).ToString("yyyy-MM-ddTHH:mm:ss")
        assignedStaffId = $staffId
        departmentId    = $deptId
    }
    
    try {
        $createRes = Invoke-RestMethod -Uri "$baseUrl/tasks/create" -Method Post -Headers $adminHeaders -Body ($newTask | ConvertTo-Json) -ContentType "application/json"
        $task = $createRes.data
        Write-Host "Created Task: $($task.id)"
    }
    catch {
        Write-Host "Error creating task: $_"
        try {
            $errorDetails = $_.Exception.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($errorDetails)
            $responseBody = $reader.ReadToEnd()
            Write-Host "Server Response: $responseBody"
        }
        catch {}

        # Fallback: List all tasks as admin and pick one
        Write-Host "Trying fallback to pick any task..."
        $allTasks = Invoke-RestMethod -Uri "$baseUrl/tasks" -Method Get -Headers $adminHeaders
        $task = $allTasks.data.content[0]
    }
}

if (-not $task) {
    Write-Host "FATAL: Could not get a task to test with."
    exit
}

Write-Host "Using Task: $($task.id) - $($task.title)"

# 3. Update Status (TO_DO -> IN_PROGRESS)
$taskId = $task.id
$statusUpdate = @{
    status = "IN_PROGRESS"
}

Write-Host "Updating status for Task ID: $taskId..."
try {
    $updateRes = Invoke-RestMethod -Uri "$baseUrl/tasks/$taskId/status" -Method Patch -Headers $staffHeaders -Body ($statusUpdate | ConvertTo-Json) -ContentType "application/json"
    Write-Host "Status Updated: $($updateRes.data.status)"
}
catch {
    Write-Host "Error updating status: $_"
    try {
        $errorDetails = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($errorDetails)
        $responseBody = $reader.ReadToEnd()
        Write-Host "Server Response: $responseBody"
    }
    catch {}
}

Write-Host "Done. Check backend logs for 'DEBUG: Make sure to check command_status output'..."
