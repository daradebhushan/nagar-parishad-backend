# verify_task_lifecycle.ps1

$baseUrl = "http://localhost:8080/api"
$authUrl = "http://localhost:8080/api/auth/login"
$adminEmail = "admin@test.com"
$password = "password"

# 1. Login
Write-Host "`n[1] Logging in..."
$loginBody = @{ email = $adminEmail; password = $password } | ConvertTo-Json
try {
    $loginRes = Invoke-RestMethod -Uri $authUrl -Method Post -Body $loginBody -ContentType "application/json"
    $token = $loginRes.data.token
    $headers = @{ Authorization = "Bearer $token" }
    Write-Host " -> Login Success."
}
catch {
    Write-Host " -> Login Failed: $_"
    exit
}

# 2. Get Departments (Data Prerequisite)
Write-Host "`n[2] Fetching Departments..."
try {
    $deptRes = Invoke-RestMethod -Uri "$baseUrl/admin/departments" -Method Get -Headers $headers
    $dept = $deptRes.data.content[0]
    $deptId = $dept.id
    Write-Host " -> Found Department: $($dept.name) (ID: $deptId)"
}
catch {
    Write-Host " -> Failed to fetch departments. Using null."
    $deptId = $null
}

# 3. Create Task
Write-Host "`n[3] Creating Task..."
$taskBody = @{
    title        = "Integration Test Task $(Get-Date -Format 'HH:mm:ss')"
    description  = "created via automated test script"
    priority     = "HIGH"
    status       = "TO_DO"
    type         = "Internal"
    departmentId = $deptId
    dueDate      = (Get-Date).AddDays(7).ToString("yyyy-MM-ddTHH:mm:ss")
} | ConvertTo-Json

try {
    $createRes = Invoke-RestMethod -Uri "$baseUrl/tasks/create" -Method Post -Body $taskBody -Headers $headers -ContentType "application/json"
    $taskId = $createRes.data.id
    Write-Host " -> Task Created Successfully. ID: $taskId"
}
catch {
    $e = $_.Exception
    if ($e.Response) {
        $reader = New-Object System.IO.StreamReader($e.Response.GetResponseStream())
        $respBody = $reader.ReadToEnd()
        Write-Host " -> API Error Body: $respBody"
    }
    Write-Host " -> Task Creation Failed: $_"
    exit
}

# 4. Get Task Details
Write-Host "`n[4] Verifying Task Details..."
try {
    $getRes = Invoke-RestMethod -Uri "$baseUrl/tasks/$taskId" -Method Get -Headers $headers
    Write-Host " -> Task Title Retrieved: $($getRes.data.title)"
}
catch {
    Write-Host " -> Failed to fetch task details: $_"
}

# 5. Add Comment
Write-Host "`n[5] Adding Comment..."
$commentBody = @{ text = "Testing comments" } | ConvertTo-Json
try {
    $commentRes = Invoke-RestMethod -Uri "$baseUrl/tasks/$taskId/comments" -Method Post -Body $commentBody -Headers $headers -ContentType "application/json"
    Write-Host " -> Comment Added."
}
catch {
    Write-Host " -> Failed to add comment: $_"
}

# 6. Delete Task
Write-Host "`n[6] Deleting Task..."
try {
    Invoke-RestMethod -Uri "$baseUrl/tasks/$taskId" -Method Delete -Headers $headers
    Write-Host " -> Task Deleted."
}
catch {
    $e = $_.Exception
    if ($e.Response) {
        $reader = New-Object System.IO.StreamReader($e.Response.GetResponseStream())
        $respBody = $reader.ReadToEnd()
        Write-Host " -> API DELETE Error Body: $respBody"
    }
    Write-Host " -> Failed to delete task: $_"
}

Write-Host "`n[OK] Backend Lifecycle Test Complete."
