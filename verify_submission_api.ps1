
$baseUrl = "http://localhost:8080/api/public"

# 1. Get Departments to find a valid ID
$depts = Invoke-RestMethod -Uri "$baseUrl/departments" -Method Get
$deptId = $depts[0].id
Write-Host "Using Department ID: $deptId"

# 2. Get Complaint Types
$types = Invoke-RestMethod -Uri "$baseUrl/complaint-types/$deptId" -Method Get
$typeId = $types[0].id
Write-Host "Using Complaint Type ID: $typeId"

# 3. Submit Complaint with Multiple Attachments
$payload = @{
    departmentId    = $deptId
    complaintTypeId = $typeId
    name            = "Multi Attach API Test"
    mobile          = "9998887777"
    description     = "Checking multiple attachments"
    location        = "API Test Location"
    attachmentUrls  = @(
        "https://placehold.co/100x100.png",
        "https://placehold.co/200x200.png"
    )
}

$jsonPayload = $payload | ConvertTo-Json -Depth 5
Write-Host "Submitting Payload: $jsonPayload"

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/complaints" -Method Post -Body $jsonPayload -ContentType "application/json"
    Write-Host "Response Received: $($response.complaintNo)"
    
    # 4. Verify Attachments
    $count = $response.attachments.Count
    Write-Host "Attachments Count: $count"
    
    if ($count -ge 2) {
        Write-Host "SUCCESS: Multiple attachments verified."
        $response.attachments | ForEach-Object {
            Write-Host " - Attachment: $($_.fileName) ($($_.fileType))"
        }
    }
    else {
        Write-Host "FAILURE: Expected at least 2 attachments, got $count"
        exit 1
    }
}
catch {
    Write-Host "Error submitting complaint: $_"
    Write-Host "Status Code: $($_.Exception.Response.StatusCode.value__)"
    $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
    Write-Host "Body: $($reader.ReadToEnd())"
    exit 1
}
