
# verify_new_admin_seeding.ps1

$baseUrl = "http://localhost:8080/api"
$ownerEmail = "bhushandarade1407+owner@gmail.com"
$ownerPassword = "Bbd@1415"

# 1. Login as Owner
echo "Logging in as Owner..."
$loginBody = @{
    email    = $ownerEmail
    password = $ownerPassword
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
    $token = $loginResponse.data.token
    echo "Owner Login Successful."
}
catch {
    echo "Owner Login Failed: $_"
    exit 1
}

$headers = @{
    Authorization  = "Bearer $token"
    "Content-Type" = "application/json"
}

# 2. Create New Admin
# Endpoint is /owner/create-admin, not /auth/register
$newAdminEmail = "testadmin_$(Get-Random)@nagar.com"
$newAdminPass = "Test@123"
echo "Creating New Admin: $newAdminEmail"

$signupBody = @{
    name     = "Test Admin Auto"
    email    = $newAdminEmail
    password = $newAdminPass
    role     = "ADMIN"
    mobile   = "1122334455"
} | ConvertTo-Json

try {
    $signupResponse = Invoke-RestMethod -Uri "$baseUrl/owner/create-admin" -Method Post -Body $signupBody -Headers $headers
    echo "New Admin Created Successfully."
}
catch {
    echo "Failed to create new Admin: $_"
    exit 1
}

# 3. Login as New Admin (to get their specific Departments)
echo "Logging in as New Admin..."
$adminLoginBody = @{
    email    = $newAdminEmail
    password = $newAdminPass
} | ConvertTo-Json

try {
    $adminLoginResponse = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $adminLoginBody -ContentType "application/json"
    $adminToken = $adminLoginResponse.data.token
    echo "New Admin Login Successful."
}
catch {
    echo "New Admin Login Failed: $_"
    exit 1
}

$adminHeaders = @{
    Authorization = "Bearer $adminToken"
}

# 4. Verify Defaults
echo "Verifying Default Departments for New Admin..."
try {
    $deptResponse = Invoke-RestMethod -Uri "$baseUrl/admin/departments" -Method Get -Headers $adminHeaders
    $depts = $deptResponse.data.content
    
    # Output Encoding
    [Console]::OutputEncoding = [System.Text.Encoding]::UTF8

    # Verify "City Cleanliness" / "शहर स्वच्छता"
    $cleanDept = $depts | Where-Object { $_.name -eq "City Cleanliness" }
    if ($cleanDept) {
        echo "SUCCESS: Found 'City Cleanliness'"
        if ($cleanDept.nameMr -like "*शहर स्वच्छता*") {
            echo "  - Marathi Name Verified: $($cleanDept.nameMr)"
        }
        else {
            echo "  - FAILURE: Marathi Name Incorrect: $($cleanDept.nameMr)"
        }
    }
    else {
        echo "FAILURE: 'City Cleanliness' NOT found."
    }

    # Verify "Waste Vehicle (Ghanta Gadi)" / "घंटागाडी"
    $ghantaDept = $depts | Where-Object { $_.name -like "*Ghanta Gadi*" }
    if ($ghantaDept) {
        echo "SUCCESS: Found 'Waste Vehicle (Ghanta Gadi)'"
        if ($ghantaDept.subQuestions -like "*मेलेले प्राणी उचलणे बाबत*") {
            echo "  - Sub-Question Verified: 'मेलेले प्राणी उचलणे बाबत' found."
        }
        else {
            echo "  - FAILURE: Sub-questions missing verification text."
            echo "  - Actual: $($ghantaDept.subQuestions)"
        }
    }
    else {
        echo "FAILURE: 'Ghanta Gadi' NOT found."
    }
    
    echo "`nFull List of Departments:"
    foreach ($d in $depts) {
        echo "- $($d.name) ($($d.nameMr))"
    }

}
catch {
    echo "Failed to fetch departments: $_"
    exit 1
}
