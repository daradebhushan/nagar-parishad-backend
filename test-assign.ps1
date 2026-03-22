$ErrorActionPreference = "Stop"
$baseUrl = "http://localhost:8080/api"

# 1. Login
$loginBody = @{
    email = "owner@govt.in"
    password = "password"
} | ConvertTo-Json
$loginResponse = Invoke-RestMethod -Uri "$baseUrl/admin/login" -Method Post -Body $loginBody -ContentType "application/json"
$token = $loginResponse.data.token
$headers = @{ "Authorization" = "Bearer $token"; "Content-Type" = "application/json" }

Write-Host "Logged in. Token: $token"

# 2. Create an unassigned task
$taskBody = @{
    title = "Test Unassigned Bug"
    description = "test desc"
    priority = "MEDIUM"
    status = "TO_DO"
    type = "Complaint"
    departmentId = 2
} | ConvertTo-Json

$createResponse = Invoke-RestMethod -Uri "$baseUrl/tasks/create" -Method Post -Body $taskBody -Headers $headers -ContentType "application/json"
$taskId = $createResponse.data.id

Write-Host "Created Task ID: $taskId"

# 3. Try to update it to assign a staff member (Staff ID 4 or 5)
$updateBody = @{
    title = "Test Unassigned Bug - Assigned"
    description = "test desc"
    priority = "MEDIUM"
    status = "TO_DO"
    type = "Complaint"
    departmentId = 2
    assignedStaffId = 5
} | ConvertTo-Json

$updateResponse = Invoke-RestMethod -Uri "$baseUrl/tasks/$taskId" -Method Put -Body $updateBody -Headers $headers -ContentType "application/json"

Write-Host "Updated Task Assigned Staff Name: $($updateResponse.data.assignedStaffName)"
Write-Host "Updated Task Assigned Staff ID: $($updateResponse.data.assignedStaffId)"

if ($updateResponse.data.assignedStaffId -eq 5) {
    Write-Host "BACKEND WORKS PERFECTLY."
} else {
    Write-Host "BACKEND BUG DETECTED."
}
