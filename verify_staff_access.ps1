# verify_staff_access.ps1

$baseUrl = "http://localhost:8080/api"
$adminEmail = "autoadmin@test.com"
$adminPassword = "password123" # match auto_admin
$staffEmail = "staff_verify@test.com"
$staffPassword = "password"

# 1. Login as Admin
Write-Host "Logging in as Admin..."
$loginBody = @{ email = $adminEmail; password = $adminPassword } | ConvertTo-Json
$response = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
$adminToken = $response.data.token
$adminHeaders = @{ Authorization = "Bearer $adminToken" }

# 2. Setup Staff User
Write-Host "Setting up Staff user..."
# Create or Get Staff
# fast way: check if exists, if not create. But easier to just create a new unique one
$staffEmail = "staff_" + (Get-Random) + "@test.com"
$staffBody = @{
    name     = "Verify Staff"
    email    = $staffEmail
    password = $staffPassword
    role     = "STAFF"
    mobile   = "9999999999"
} | ConvertTo-Json

try {
    Invoke-RestMethod -Uri "$baseUrl/admin/users" -Method Post -Body $staffBody -ContentType "application/json" -Headers $adminHeaders
    Write-Host "Created Staff: $staffEmail"
}
catch {
    Write-Host "Staff creation failed: $_" -ForegroundColor Red
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader $_.Exception.Response.GetResponseStream()
        $errBody = $reader.ReadToEnd()
        Write-Host "Error Body: $errBody" -ForegroundColor Yellow
    }
}

# Login as Staff
$staffLoginBody = @{ email = $staffEmail; password = $staffPassword } | ConvertTo-Json
$staffRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $staffLoginBody -ContentType "application/json"
$staffToken = $staffRes.data.token
$staffHeaders = @{ Authorization = "Bearer $staffToken" }
$staffId = $staffRes.data.id

# 3. Create Complaint
Write-Host "Creating Complaint..."
$complaintBody = @{
    name            = "Test Citizen"
    mobile          = "1234567890"
    departmentId    = 1 # Assuming dept 1 exists
    complaintTypeId = 1 # Assuming type 1 exists
    description     = "Original Complaint Description"
} | ConvertTo-Json
$complaint = Invoke-RestMethod -Uri "$baseUrl/public/complaints" -Method Post -Body $complaintBody -ContentType "application/json"
$complaintId = $complaint.id
Write-Host "Complaint Created: ID $complaintId"

# 4. Convert to Task (Empty Description)
if (!$complaintId) {
    Write-Host "ERROR: Complaint ID is null. Stopping." -ForegroundColor Red
    exit
}

Write-Host "Converting to Task (Empty Description)..."
$taskBody = @{
    title           = "Converted Task"
    description     = ""
    priority        = "MEDIUM"
    status          = "TO_DO"
    assignedStaffId = $staffId
    departmentId    = 1
} | ConvertTo-Json

$converted = Invoke-RestMethod -Uri "$baseUrl/admin/complaints/$complaintId/create-task" -Method Post -Body $taskBody -ContentType "application/json" -Headers $adminHeaders
$taskId = $converted.relatedTaskId
Write-Host "Task Created: ID $taskId"

# 5. Verify Description Copy
$task = Invoke-RestMethod -Uri "$baseUrl/tasks/$taskId" -Method Get -Headers $adminHeaders
Write-Host "Full Task Response:"
$task | ConvertTo-Json -Depth 5
$taskDesc = $task.description
if ($taskDesc -match "Original Complaint Description") {
    Write-Host "SUCCESS: Description copied correctly!" -ForegroundColor Green
    Write-Host "Desc: $taskDesc"
}
else {
    Write-Host "FAILURE: Description NOT copied!" -ForegroundColor Red
    Write-Host "Desc: $taskDesc"
}

# 6. Verify Staff Access (Assigned)
Write-Host "Verifying Staff Access (Assigned)..."
try {
    $c = Invoke-RestMethod -Uri "$baseUrl/admin/complaints/$complaintId" -Method Get -Headers $staffHeaders
    Write-Host "SUCCESS: Staff could access assigned complaint." -ForegroundColor Green
}
catch {
    Write-Host "FAILURE: Staff could NOT access assigned complaint." -ForegroundColor Red
    Write-Host $_
}

# 7. Verify Staff Access (Unassigned)
Write-Host "Verifying Staff Access (Unassigned)..."
# Create another complaint
$c2 = Invoke-RestMethod -Uri "$baseUrl/public/complaints" -Method Post -Body $complaintBody -ContentType "application/json"
$c2Id = $c2.data.id

try {
    Invoke-RestMethod -Uri "$baseUrl/admin/complaints/$c2Id" -Method Get -Headers $staffHeaders
    Write-Host "FAILURE: Staff accessed unassigned complaint!" -ForegroundColor Red
}
catch {
    if ($_.Exception.Response.StatusCode -eq "Forbidden" -or $_.Exception.Response.StatusCode -eq "Unauthorized") {
        Write-Host "SUCCESS: Staff blocked from unassigned complaint ($($_.Exception.Response.StatusCode))." -ForegroundColor Green
    }
    else {
        Write-Host "WARNING: Unexpected error code: $($_.Exception.Response.StatusCode)" -ForegroundColor Yellow
    }
}
