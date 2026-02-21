
# Configurations
$baseUrl = "http://localhost:8080/api"
$adminEmail = "autoadmin@test.com"
$adminPassword = "password123"

# 1. Login as Admin
Write-Host "Logging in as Admin..."
$loginBody = @{
    email    = $adminEmail
    password = $adminPassword
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
    $adminToken = $loginResponse.data.token
    $adminHeaders = @{ Authorization = "Bearer $adminToken" }
    Write-Host "Admin Login Successful."
}
catch {
    Write-Error "Admin Login Failed: $_"
    try {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $body = $reader.ReadToEnd()
        Write-Host "Error Body: $body" -ForegroundColor Red
    }
    catch {}
    exit
}

# 2. Create Staff User
$staffEmail = "staff_list_" + (Get-Random) + "@test.com"
Write-Host "Creating Staff: $staffEmail"

$staffBody = @{
    name     = "List Staff"
    mobile   = "9999999999"
    email    = $staffEmail
    password = "password123"
    role     = "STAFF"
} | ConvertTo-Json

try {
    $staffResponse = Invoke-RestMethod -Uri "$baseUrl/admin/users" -Method Post -Body $staffBody -ContentType "application/json" -Headers $adminHeaders
    $staffId = $staffResponse.data.id
    Write-Host "Staff Created: ID $staffId"
}
catch {
    Write-Error "Staff Creation Failed: $_"
    exit
}

# 3. Create Two Complaints
Write-Host "Creating Complaints..."
$complaintBody = @{
    name            = "Citizen List Test"
    mobile          = "9876543210"
    departmentId    = 6
    complaintTypeId = 1
    description     = "Test Complaint for List"
} | ConvertTo-Json

try {
    $c1 = Invoke-RestMethod -Uri "$baseUrl/public/complaints" -Method Post -Body $complaintBody -ContentType "application/json"
    $c2 = Invoke-RestMethod -Uri "$baseUrl/public/complaints" -Method Post -Body $complaintBody -ContentType "application/json"
    Write-Host "Complaints Created: ID $($c1.id) and ID $($c2.id)"
}
catch {
    Write-Error "Complaint Creation Failed: $_"
    exit
}

# 4. Create Task for Complaint 1 -> Assign to Staff
Write-Host "Assigning Complaint ID $($c1.id) to Staff..."
$taskBody = @{
    title           = "Task for Complaint 1"
    priority        = "HIGH"
    status          = "TO_DO"
    dueDate         = (Get-Date).AddDays(2).ToString("yyyy-MM-ddTHH:mm:ss")
    departmentId    = 6
    assignedStaffId = $staffId
} | ConvertTo-Json

try {
    $t1 = Invoke-RestMethod -Uri "$baseUrl/admin/complaints/$($c1.id)/create-task" -Method Post -Body $taskBody -ContentType "application/json" -Headers $adminHeaders
    Write-Host "Task Created for C1: Assigned to Staff."
}
catch {
    Write-Error "Task Creation 1 Failed: $_"
    try {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $body = $reader.ReadToEnd()
        Write-Host "Error Body: $body" -ForegroundColor Red
    }
    catch {}
    exit
}

# 5. Create Task for Complaint 2 -> Assign to NO ONE (or Admin)
Write-Host "Converting Complaint ID $($c2.id) to Task (Unassigned to Staff)..."
$taskBody2 = @{
    title        = "Task for Complaint 2"
    priority     = "LOW"
    status       = "TO_DO"
    # dueDate      = (Get-Date).AddDays(2).ToString("yyyy-MM-ddTHH:mm:ss")
    departmentId = 6
    # No assignedStaffId
} | ConvertTo-Json

try {
    $t2 = Invoke-RestMethod -Uri "$baseUrl/admin/complaints/$($c2.id)/create-task" -Method Post -Body $taskBody2 -ContentType "application/json" -Headers $adminHeaders
    Write-Host "Task Created for C2: Not Assigned to Staff."
}
catch {
    Write-Error "Task Creation 2 Failed: $_"
    exit
}

# 6. Login as Staff
Write-Host "Logging in as Staff..."
$staffLoginBody = @{
    email    = $staffEmail
    password = "password123"
} | ConvertTo-Json

try {
    $staffLoginResp = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $staffLoginBody -ContentType "application/json"
    $staffToken = $staffLoginResp.data.token
    $staffHeaders = @{ Authorization = "Bearer $staffToken" }
    Write-Host "Staff Login Successful."
}
catch {
    Write-Error "Staff Login Failed: $_"
    exit
}

# 7. Get Complaint List as Staff
Write-Host "Fetching Complaint List as Staff..."
try {
    $list = Invoke-RestMethod -Uri "$baseUrl/admin/complaints" -Method Get -Headers $staffHeaders
    Write-Host "Complaints Found: $($list.Count)"
    
    $foundC1 = $false
    $foundC2 = $false
    
    foreach ($item in $list) {
        if ($item.id -eq $c1.id) { $foundC1 = $true }
        if ($item.id -eq $c2.id) { $foundC2 = $true }
    }
    
    if ($foundC1 -and -not $foundC2) {
        Write-Host "SUCCESS: Staff sees assigned complaint ($($c1.id)) but NOT unassigned ($($c2.id))." -ForegroundColor Green
    }
    elseif ($foundC2) {
        Write-Host "FAILURE: Staff sees unassigned complaint ($($c2.id))!" -ForegroundColor Red
    }
    else {
        Write-Host "FAILURE: Staff could not find assigned complaint ($($c1.id))." -ForegroundColor Red
    }

}
catch {
    Write-Error "Get List Failed: $_"
    exit
}
