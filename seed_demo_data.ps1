# seed_demo_data.ps1
$baseUrl = "https://repayable-kenisha-inapprehensively.ngrok-free.dev/api"
$ownerCreds = @{ email = "daradebhushan15+admin@gmail.com"; password = "Bbd@1415" }
# LOGIN AS ADMIN

# 1. Login
Write-Host "Logging in..."
$login = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body ($ownerCreds | ConvertTo-Json) -ContentType "application/json"
$token = $login.data.token
$headers = @{ Authorization = "Bearer $token" }

# 2. Get Dependencies
Write-Host "Fetching Departments..."
$deptRes = Invoke-RestMethod -Uri "$baseUrl/public/departments" -Method Get -Headers $headers
$depts = if ($deptRes.data) { $deptRes.data } else { $deptRes }
$deptId = if ($depts.Count -gt 0) { $depts[0].id } else { $null }

Write-Host "Fetching Designations..."
$desigRes = Invoke-RestMethod -Uri "$baseUrl/public/designations" -Method Get -Headers $headers
$desigs = if ($desigRes.data) { $desigRes.data } else { $desigRes }
$desigId = if ($desigs.Count -gt 0) { $desigs[0].id } else { $null }

if (-not $deptId -or -not $desigId) {
    Write-Host "WARNING: No Departments or Designations found. Seeding might fail or need simpler user."
    # Try creating one? 
    # For now, just try to continue, maybe they are not required.
}

# 3. Create Staff
Write-Host "Creating Staff Member..."
$staffBody = @{
    name          = "Demo Staff"
    email         = "staff@demo.com"
    password      = "password"
    mobile        = "9876543210"
    role          = "STAFF"
    active        = $true
    departmentId  = $deptId
    designationId = $desigId
} | ConvertTo-Json

try {
    # Use Admin Endpoint which automatically assigns the logged-in user as Admin
    Invoke-RestMethod -Uri "$baseUrl/admin/users" -Method Post -Body $staffBody -ContentType "application/json" -Headers $headers
    Write-Host "Staff Created Successfully (Linked to Admin)."
}
catch {
    Write-Host "Staff Creation FAILED: $_"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        Write-Host "Response Body: $($reader.ReadToEnd())"
    }
}

# 4. Create Complaint
Write-Host "Creating Demo Complaint..."
# Need a complaint type ID.
$typesRes = Invoke-RestMethod -Uri "$baseUrl/public/complaint-types/$deptId" -Method Get -Headers $headers
$types = if ($typesRes.data) { $typesRes.data } else { $typesRes }
$typeId = if ($types.Count -gt 0) { $types[0].id } else { $null }

if ($typeId) {
    $compBody = @{
        name            = "John Doe"
        mobile          = "9999999999"
        location        = "123 Main St"
        departmentId    = $deptId
        complaintTypeId = $typeId
        description     = "This is a test complaint seeded by script."
    } | ConvertTo-Json

    try {
        Invoke-RestMethod -Uri "$baseUrl/public/complaints" -Method Post -Body $compBody -ContentType "application/json"
        Write-Host "Complaint Created Successfully."
    }
    catch {
        Write-Host "Complaint Creation FAILED: $_"
        if ($_.Exception.Response) {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            Write-Host "Body: $($reader.ReadToEnd())"
        }
    }
}
else {
    Write-Host "No Complaint Types found, skipping complaint creation."
}

# Final Check
$users = Invoke-RestMethod -Uri "$baseUrl/admin/users" -Method Get -Headers $headers
Write-Host "Final User Count: $($users.data.content.Count)"

$complaints = Invoke-RestMethod -Uri "$baseUrl/admin/complaints" -Method Get -Headers $headers
Write-Host "Final Complaint Count: $($complaints.Count)"
