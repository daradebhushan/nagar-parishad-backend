
# verify_complaint_task_linking.ps1

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

# 2. Find or Create Complaint
echo "Creating Test Complaint..."
try {
    # Check for existing PENDING
    $complaints = Invoke-RestMethod -Uri "$baseUrl/admin/complaints" -Method Get -Headers $headers
    $pendingComplaint = $complaints.data | Where-Object { $_.status -eq "PENDING" } | Select-Object -First 1
    
    if (-not $pendingComplaint) {
        echo "No PENDING complaints found. Creating a test complaint..."
        $complaintBody = @{
            name            = "Script Tester"
            mobile          = "9876543210"
            departmentId    = 1
            complaintTypeId = 1
            description     = "Auto-generated for verification"
            location        = "Script Location"
        } | ConvertTo-Json

        try {
            $createResponse = Invoke-RestMethod -Uri "$baseUrl/public/complaints" -Method Post -Body $complaintBody -ContentType "application/json"
            $complaintId = $createResponse.id
            if (-not $complaintId) { $complaintId = $createResponse.data.id } # Handle if wrapped in ApiResponse
            echo "Created New Complaint ID: $complaintId"
        }
        catch {
            echo "Failed to create complaint: $_"
            exit 1
        }
    }
    else {
        $complaintId = $pendingComplaint.id
        echo "Using Existing Complaint ID: $complaintId (Status: $($pendingComplaint.status))"
    }

}
catch {
    echo "Failed to fetch/create complaint: $_"
    exit 1
}

# 3. Create a Task Linked to this Complaint
echo "Creating Task linked to Complaint ID: $complaintId..."
$taskBody = @{
    title       = "Task from Complaint $complaintId"
    description = "Fix the issue reported in complaint $complaintId"
    priority    = "HIGH"
    status      = "TO_DO"
    type        = "Complaint"
    complaintId = $complaintId
    dueDate     = (Get-Date).AddDays(7).ToString("yyyy-MM-ddTHH:mm:ss")
} | ConvertTo-Json

try {
    $taskResponse = Invoke-RestMethod -Uri "$baseUrl/tasks/create" -Method Post -Body $taskBody -Headers $headers
    $task = $taskResponse.data
    echo "Task Created Successfully. Task ID: $($task.id)"
}
catch {
    echo "Failed to create task: $_"
    echo $_.Exception.Response.GetResponseStream() 
    exit 1
}

# 4. Verify Linking
echo "Verifying Complaint Status..."
try {
    $updatedComplaint = Invoke-RestMethod -Uri "$baseUrl/admin/complaints/$complaintId" -Method Get -Headers $headers
    
    # Debug JSON
    echo "DEBUG JSON: $($updatedComplaint | ConvertTo-Json -Depth 5)"
    
    $status = $updatedComplaint.data.status
    $relatedTaskId = $updatedComplaint.data.relatedTaskId
    
    echo "Complaint Status: $status"
    echo "Related Task ID: $relatedTaskId"
    
    if ($status -eq "CONVERTED_TO_TASK") {
        echo "SUCCESS: Complaint Status updated correctly."
    }
    else {
        echo "FAILURE: Complaint Status is '$status' (Expect: CONVERTED_TO_TASK)"
    }
    
    if ($relatedTaskId -eq $task.id) {
        echo "SUCCESS: Complaint is linked to Task $($task.id)."
    }
    else {
        echo "FAILURE: Complaint linked task ID ($relatedTaskId) does not match created task."
    }

}
catch {
    echo "Failed to verify complaint: $_"
    exit 1
}
