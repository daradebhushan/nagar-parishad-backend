$baseUrl = "http://127.0.0.1:8080/api"
$logFile = "d:\anti gravity\backend\crud_results.txt"

function Log-Output($message) {
    Write-Output $message
    Add-Content -Path $logFile -Value $message
}

Clear-Content -Path $logFile -ErrorAction SilentlyContinue

Log-Output "--- E2E DEPARTMENT & STAFF CRUD VALIDATION SUITE ---`n"

$ownerEmail = "owner@govt.in"
$ownerPassword = "password"
$ownerBody = @{ email = $ownerEmail; password = $ownerPassword } | ConvertTo-Json

# Log in as Owner to fetch or provision a Chief Officer
try {
    $loginData = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $ownerBody -ContentType "application/json"
    $ownerToken = $loginData.data.token
}
catch {
    Log-Output "FAILED: Could not login as system owner."
    exit 1
}

$headersOwner = @{ Authorization = "Bearer $ownerToken" }

# Fetch CO
$usersRes = Invoke-RestMethod -Uri "$baseUrl/admin/users" -Method Get -Headers $headersOwner
$coUser = $usersRes.data.content | Where-Object { $_.role -eq "ADMIN" } | Select-Object -First 1

if (-not $coUser) {
    Log-Output "No Chief Officer found. Creating temporary..."
    $coBody = @{
        name     = "Test CO for CRUD"
        email    = "co_crud@nagarparishad.in"
        password = "password123"
        mobile   = "9998887771"
        role     = "ADMIN"
        active   = $true
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/admin/users" -Method Post -Body $coBody -ContentType "application/json" -Headers $headersOwner | Out-Null
    $coEmail = "co_crud@nagarparishad.in"
    $coPassword = "password123"
}
else {
    $coEmail = $coUser.email
    $updateBody = @{ password = "password123" } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/admin/users/$($coUser.id)" -Method Put -Body $updateBody -ContentType "application/json" -Headers $headersOwner | Out-Null
    $coPassword = "password123"
}

# 1. Login as CO
$coLoginBody = @{ email = $coEmail; password = $coPassword } | ConvertTo-Json
$coLoginRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $coLoginBody -ContentType "application/json"
$coToken = $coLoginRes.data.token
$headersCo = @{ Authorization = "Bearer $coToken" }
Log-Output "SUCCESS: Chief Officer Logged In."

Log-Output "`n--- 1. DEPARTMENT CRUD ---"

# CREATE DEPT
$deptBody = @{ name = "E2E Test Dept"; chatbotMarathiLabel = "Test Marathi" } | ConvertTo-Json
try {
    $newDept = Invoke-RestMethod -Uri "$baseUrl/admin/departments" -Method Post -Body $deptBody -ContentType "application/json" -Headers $headersCo
    $deptId = $newDept.data.id
    Log-Output "SUCCESS: [CREATE] Department created (ID: $deptId)"
}
catch {
    Log-Output "FAILED: [CREATE] Department: $($_.Exception.Message)"
    exit 1
}

# READ DEPT
try {
    $depts = Invoke-RestMethod -Uri "$baseUrl/public/departments" -Method Get
    $foundDept = $depts | Where-Object { $_.id -eq $deptId }
    if ($foundDept) {
        Log-Output "SUCCESS: [READ] Department exists in public API list."
    }
    else {
        Log-Output "FAILED: [READ] Department not found in public list."
    }
}
catch {
    Log-Output "FAILED: [READ] Department API Error: $($_.Exception.Message)"
}

# UPDATE DEPT
$deptUpdateBody = @{ name = "E2E Test Dept (Updated)"; chatbotMarathiLabel = "Updated" } | ConvertTo-Json
try {
    $updatedDept = Invoke-RestMethod -Uri "$baseUrl/admin/departments/$deptId" -Method Put -Body $deptUpdateBody -ContentType "application/json" -Headers $headersCo
    Log-Output "SUCCESS: [UPDATE] Department Name changed to: $($updatedDept.data.name)"
}
catch {
    Log-Output "FAILED: [UPDATE] Department: $($_.Exception.Message)"
}


Log-Output "`n--- 2. STAFF WORKFLOW CRUD ---"

# CREATE STAFF
$staffEmail = "test_staff_crud_$((Get-Date).Ticks)@demo.com"
$staffBody = @{
    name         = "CRUD Test Staff"
    email        = $staffEmail
    password     = "password123"
    mobile       = "9876543211"
    role         = "STAFF"
    active       = $true
    departmentId = $deptId
} | ConvertTo-Json

try {
    $newStaff = Invoke-RestMethod -Uri "$baseUrl/admin/users" -Method Post -Body $staffBody -ContentType "application/json" -Headers $headersCo
    $staffId = $newStaff.data.id
    Log-Output "SUCCESS: [CREATE] Staff User created (ID: $staffId)"
}
catch {
    Log-Output "FAILED: [CREATE] Staff User: $($_.Exception.Message)"
}

# READ STAFF
try {
    $allUsers = Invoke-RestMethod -Uri "$baseUrl/admin/users?size=100" -Method Get -Headers $headersCo
    $foundStaff = $allUsers.data.content | Where-Object { $_.id -eq $staffId }
    if ($foundStaff) {
        Log-Output "SUCCESS: [READ] Staff User retrieved. Found: $($foundStaff.name) assigned to Dept ID: $($foundStaff.department.id)"
    }
    else {
        Log-Output "FAILED: [READ] Staff User not found in Users list."
    }
}
catch {
    Log-Output "FAILED: [READ] Staff: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        Log-Output "JSON RESPONSE: $($reader.ReadToEnd())"
    }
}

# UPDATE STAFF
$staffUpdateBody = @{ name = "CRUD Test Staff (Updated)"; active = $false } | ConvertTo-Json
try {
    $updatedStaff = Invoke-RestMethod -Uri "$baseUrl/admin/users/$staffId" -Method Put -Body $staffUpdateBody -ContentType "application/json" -Headers $headersCo
    Log-Output "SUCCESS: [UPDATE] Staff User modified (Name: $($updatedStaff.data.name), Active: $($updatedStaff.data.active))"
}
catch {
    Log-Output "FAILED: [UPDATE] Staff: $($_.Exception.Message)"
}

# DELETE STAFF
try {
    Invoke-RestMethod -Uri "$baseUrl/admin/users/$staffId" -Method Delete -Headers $headersCo | Out-Null
    Log-Output "SUCCESS: [DELETE] Staff User completely erased."
}
catch {
    Log-Output "FAILED: [DELETE] Staff User: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        Log-Output "JSON RESPONSE: $($reader.ReadToEnd())"
    }
}

Log-Output "`n--- 3. CLEANUP DEPARTMENT ---"

# DELETE DEPT
try {
    Invoke-RestMethod -Uri "$baseUrl/admin/departments/$deptId" -Method Delete -Headers $headersCo | Out-Null
    Log-Output "SUCCESS: [DELETE] Department successfully deleted after cascading empty."
}
catch {
    Log-Output "FAILED: [DELETE] Department: $($_.Exception.Message)"
}

Log-Output "`n--- TESTS COMPLETED ---"
