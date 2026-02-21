
# seed_departments.ps1

$baseUrl = "http://localhost:8080/api"
$adminEmail = "admin@nagarparishad.in"
$password = "password"

# 1. Login
echo "Logging in as Admin..."
$loginBody = @{
    email    = $adminEmail
    password = $password
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
    $token = $loginResponse.data.token
    echo "Login Successful. Token received."
}
catch {
    echo "Login Failed: $_"
    exit 1
}

$headers = @{
    Authorization  = "Bearer $token"
    "Content-Type" = "application/json"
}

# 2. Reset Departments (Delete old encoded ones)
echo "Resetting Departments..."
try {
    $deptResponse = Invoke-RestMethod -Uri "$baseUrl/admin/departments" -Method Get -Headers $headers
    $depts = $deptResponse.data.content

    foreach ($dept in $depts) {
        if ($dept.name -match "Ghanta Gadi|City Cleanliness|Water Supply|Electricity|Public Works") {
            echo "Deleting old department: $($dept.name) (ID: $($dept.id))"
            try {
                Invoke-RestMethod -Uri "$baseUrl/admin/departments/$($dept.id)" -Method Delete -Headers $headers
            }
            catch {
                echo "Failed to delete $($dept.name): $_"
            }
        }
    }
}
catch {
    echo "Failed to fetch existing departments for cleanup: $_"
    # Continue anyway to try seeding
}

# 3. Trigger Seeding
echo "Triggering Seeding Endpoint..."
try {
    $seedResponse = Invoke-RestMethod -Uri "$baseUrl/admin/departments/seed-defaults" -Method Post -Headers $headers
    echo "Seeding/Update triggered successfully."
}
catch {
    echo "Seeding Failed: $_"
    exit 1
}

# 4. Verify Departments
echo "Verifying Departments..."
try {
    $deptResponse = Invoke-RestMethod -Uri "$baseUrl/admin/departments" -Method Get -Headers $headers
    $depts = $deptResponse.data.content
    
    # Check for "Waste Vehicle (Ghanta Gadi)" or equivalent
    $ghantaGadi = $depts | Where-Object { $_.name -like "*Ghanta Gadi*" -or $_.nameMr -like "*घंटागाडी*" }
    
    if ($ghantaGadi) {
        echo "SUCCESS: Found Department 'Ghanta Gadi'"
        # Verify sub-questions
        # Use Output-Encoding to ensure console prints correctly if possible
        [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
        
        if ($ghantaGadi.subQuestions -like "*मेलेले प्राणी उचलणे बाबत*") {
            echo "SUCCESS: Verified Sub-Question 'मेलेले प्राणी उचलणे बाबत'"
        }
        else {
            echo "FAILURE: Sub-questions do not match expected Marathi text."
            echo "Actual: $($ghantaGadi.subQuestions)"
        }
    }
    else {
        echo "FAILURE: Could not find 'Ghanta Gadi' department."
    }
    
    # Check for "City Cleanliness"
    $cleanliness = $depts | Where-Object { $_.name -like "*City Cleanliness*" }
    if ($cleanliness) {
        echo "SUCCESS: Found Department 'City Cleanliness' with Marathi name: $($cleanliness.nameMr)"
    }

}
catch {
    echo "Failed to fetch departments: $_"
    exit 1
}
