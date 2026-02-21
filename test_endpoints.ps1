$baseUrl = "http://localhost:8080/api"

function Invoke-Api {
    param(
        [string]$Url,
        [string]$Method = "GET",
        [hashtable]$Headers = @{},
        [string]$Body = $null
    )
    
    try {
        $params = @{
            Uri         = $Url
            Method      = $Method
            ContentType = "application/json"
            Headers     = $Headers
        }
        if ($Body) { $params.Body = $Body }
        
        $response = Invoke-RestMethod @params
        return $response
    }
    catch {
        Write-Host "Error calling $Url" -ForegroundColor Red
        Write-Host $_.Exception.Message -ForegroundColor Red
        if ($_.Exception.Response) {
            $reader = New-Object System.IO.StreamReader $_.Exception.Response.GetResponseStream()
            $errBody = $reader.ReadToEnd()
            Write-Host "Response Body: $errBody" -ForegroundColor DarkRed
        }
        return $null
    }
}

Write-Host "--- 0. Setup New Owner (fresh) ---" -ForegroundColor Cyan
$ownerSetup = @{
    name     = "Super Owner 2"
    email    = "owner2@govt.in"
    password = "password"
    role     = "OWNER"
    mobile   = "1234567890"
} | ConvertTo-Json

$setupRes = Invoke-Api -Url "$baseUrl/auth/setup-owner" -Method "POST" -Body $ownerSetup
if ($setupRes) {
    Write-Host "Owner 2 Registered Successfully!" -ForegroundColor Green
}

Write-Host "--- 1. Login as Owner ---" -ForegroundColor Cyan
$ownerLogin = @{
    email    = "owner2@govt.in"
    password = "password"
} | ConvertTo-Json

$ownerRes = Invoke-Api -Url "$baseUrl/auth/login" -Method "POST" -Body $ownerLogin
if ($ownerRes) {
    Write-Host "Owner Login Success!" -ForegroundColor Green
    $ownerToken = $ownerRes.data.token
    # Write-Host "Token: $ownerToken"
}
else {
    Write-Host "Owner Login Failed even after registration" -ForegroundColor Red
    exit
}

Write-Host "`n--- 2. Create Admin (By Owner) ---" -ForegroundColor Cyan
$adminHeaders = @{ Authorization = "Bearer $ownerToken" }
$newAdmin = @{
    name     = "Test Admin"
    email    = "testadmin@city.in"
    password = "password"
    role     = "ADMIN"
} | ConvertTo-Json

$createAdminRes = Invoke-Api -Url "$baseUrl/owner/create-admin" -Method "POST" -Headers $adminHeaders -Body $newAdmin
if ($createAdminRes) {
    Write-Host "Admin Created Successfully!" -ForegroundColor Green
    $adminId = $createAdminRes.data.id
}

Write-Host "`n--- 3. Login as New Admin ---" -ForegroundColor Cyan
$adminLogin = @{
    email    = "testadmin@city.in"
    password = "password"
} | ConvertTo-Json

$adminRes = Invoke-Api -Url "$baseUrl/auth/login" -Method "POST" -Body $adminLogin
if ($adminRes) {
    Write-Host "Admin Login Success!" -ForegroundColor Green
    $adminToken = $adminRes.data.token
}
else {
    exit
}

$adminAuthHeaders = @{ Authorization = "Bearer $adminToken" }

Write-Host "`n--- 4. Create Department (By Admin) ---" -ForegroundColor Cyan
$newDept = @{
    name = "IT Department"
} | ConvertTo-Json

$deptRes = Invoke-Api -Url "$baseUrl/admin/departments" -Method "POST" -Headers $adminAuthHeaders -Body $newDept
if ($deptRes) {
    Write-Host "Department Created: $($deptRes.data.name) (ID: $($deptRes.data.id))" -ForegroundColor Green
    $deptId = $deptRes.data.id
}

Write-Host "`n--- 5. Create Task (By Admin) ---" -ForegroundColor Cyan
$newTask = @{
    title        = "Fix Server Issues"
    description  = "Production server is running slow"
    priority     = "CRITICAL"
    status       = "PENDING"
    departmentId = $deptId
    type         = "Maintenance"
} | ConvertTo-Json

$taskRes = Invoke-Api -Url "http://localhost:8080/api/tasks/create" -Method "POST" -Headers $adminAuthHeaders -Body $newTask
if ($taskRes) {
    Write-Host "Task Created: $($taskRes.data.title) (ID: $($taskRes.data.id))" -ForegroundColor Green
    $taskId = $taskRes.data.id
}

Write-Host "`n--- 6. Get All Tasks (By Admin) ---" -ForegroundColor Cyan
$taskListRes = Invoke-Api -Url "http://localhost:8080/api/tasks/filter" -Headers $adminAuthHeaders
if ($taskListRes) {
    $count = $taskListRes.data.content.Count
    Write-Host "Fetched $count tasks." -ForegroundColor Green
}

Write-Host "`n--- 7. Add Comment (By Admin) ---" -ForegroundColor Cyan
$comment = @{
    text = "Initial investigation started."
} | ConvertTo-Json

$commentRes = Invoke-Api -Url "http://localhost:8080/api/tasks/$taskId/comments" -Method "POST" -Headers $adminAuthHeaders -Body $comment
if ($commentRes) {
    Write-Host "Comment Added: $($commentRes.data.text)" -ForegroundColor Green
}

Write-Host "`n--- 8. Upload Comment Attachment (By Admin) ---" -ForegroundColor Cyan
# Create a dummy file
$filePath = "test_attachment.txt"
"This is a test attachment content" | Set-Content $filePath

# Fetch comment ID (assuming we just added one and it's the first one, or we can fetch list)
# For simplicity, we'll fetch comments and get the ID of the last one
$commentsRes = Invoke-Api -Url "http://localhost:8080/api/tasks/$taskId/comments" -Headers $adminAuthHeaders
if ($commentsRes -and $commentsRes.data.Count -gt 0) {
    $commentId = $commentsRes.data[$commentsRes.data.Count - 1].id
    Write-Host "Target Comment ID: $commentId" -ForegroundColor Yellow

    # Upload File logic in PowerShell is tricky with Invoke-RestMethod for multipart
    # We will use curl for this specific step if available, or a robust PS function.
    # Let's try a simple curl command usage since it's Windows 10+ usually has curl.exe
    
    $uploadUrl = "http://localhost:8080/api/tasks/$taskId/comments/$commentId/attachments"
    $authToken = "Bearer $adminToken"
    
    # Using curl.exe provided by Windows/Git Bash
    Write-Host "Executing curl to upload file..."
    $curlOutput = curl.exe -X POST $uploadUrl -H "Authorization: $authToken" -F "file=@$filePath"
    Write-Host "Curl Output: $curlOutput" -ForegroundColor Green
}
else {
    Write-Host "No comments found to attach file to." -ForegroundColor Red
}


Write-Host "`n--- 9. Get Dashboard Stats (By Admin) ---" -ForegroundColor Cyan
$statsRes = Invoke-Api -Url "http://localhost:8080/api/stats/dashboard" -Headers $adminAuthHeaders
if ($statsRes) {
    Write-Host "Dashboard Stats Fetched!" -ForegroundColor Green
    Write-Host "Total Depts: $($statsRes.data.totalDepartments)"
    Write-Host "Total Users: $($statsRes.data.totalUsers)"
    Write-Host "Total Tasks: $($statsRes.data.totalTasks)"
    Write-Host "Pending Tasks: $($statsRes.data.pendingTasks)"
}

Write-Host "`n--- TEST SUITE COMPLETED ---" -ForegroundColor Magenta

