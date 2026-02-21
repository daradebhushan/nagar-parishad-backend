
# verify_subissues.ps1

$baseUrl = "http://localhost:8080/api"
$adminEmail = "admin@nagarparishad.in"
$password = "password"

# 1. Login
$loginBody = @{ email = $adminEmail; password = $password } | ConvertTo-Json
try {
    $loginResponse = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
    $token = $loginResponse.data.token
}
catch {
    echo "Login Failed"
    exit 1
}

$headers = @{ Authorization = "Bearer $token" }

# 2. Fetch Departments
try {
    $deptResponse = Invoke-RestMethod -Uri "$baseUrl/admin/departments" -Method Get -Headers $headers
    $depts = $deptResponse.data.content

    # Output Encoding for Marathi
    [Console]::OutputEncoding = [System.Text.Encoding]::UTF8

    echo "`n=== VERIFICATION REPORT: DEPARTMENTS & SUB-ISSUES ===`n"

    foreach ($dept in $depts) {
        echo "Department: $($dept.name) ($($dept.nameMr))"
        echo "------------------------------------------------"
        
        # SubQuestions is a JSON string, let's try to parse it specifically
        try {
            $questions = $dept.subQuestions | ConvertFrom-Json
            foreach ($q in $questions) {
                echo "  - $q"
            }
        }
        catch {
            echo "  [Raw Text]: $($dept.subQuestions)"
        }
        echo "`n"
    }
}
catch {
    echo "Failed to fetch data: $_"
}
