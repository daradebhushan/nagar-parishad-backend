
# 1. Get Latest Complaint ID
$all = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/complaints" -Method Get -Headers @{ "Authorization" = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiQURNSU4iLCJpZCI6NDgsInN1YiI6ImFoZXJjaGV0YW5hMkBnbWFpbC5jb20iLCJpYXQiOjE3Njg3MTQyNjQsImV4cCI6MTc2ODgwMDY2NH0.L9as7LzN22zZsfzTXgiJ77VJ0GThHpU6LWJk68jIG60" }
$latest = $all | Sort-Object -Property id -Descending | Select-Object -First 1
$id = $latest.id
if ($id -is [array]) { $id = $id[0] } # Safety check
Write-Host "Testing on Complaint ID: $id"

# 2. Add Comment
Write-Host "Adding Comment..."
$commentBody = @{ text = "Investigating this issue." } | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8080/api/admin/complaints/$id/comments" -Method Post -Body $commentBody -ContentType "application/json" -Headers @{ "Authorization" = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiQURNSU4iLCJpZCI6NDgsInN1YiI6ImFoZXJjaGV0YW5hMkBnbWFpbC5jb20iLCJpYXQiOjE3Njg3MTQyNjQsImV4cCI6MTc2ODgwMDY2NH0.L9as7LzN22zZsfzTXgiJ77VJ0GThHpU6LWJk68jIG60" }

# 3. Get Details & Verify Comment
Write-Host "Verifying Comment..."
$details = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/complaints/$id" -Method Get -Headers @{ "Authorization" = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiQURNSU4iLCJpZCI6NDgsInN1YiI6ImFoZXJjaGV0YW5hMkBnbWFpbC5jb20iLCJpYXQiOjE3Njg3MTQyNjQsImV4cCI6MTc2ODgwMDY2NH0.L9as7LzN22zZsfzTXgiJ77VJ0GThHpU6LWJk68jIG60" }
$comments = $details.comments
Write-Host "Comments Found: $($comments.Count)"
if ($comments[0].text -eq "Investigating this issue.") {
    Write-Host "SUCCESS: Comment added." -ForegroundColor Green
}
else {
    Write-Host "FAILURE: Comment mismatch." -ForegroundColor Red
}

# 4. Convert to Task
Write-Host "Converting to Task..."
$taskPayload = @{
    title        = "Fix Complaint $id"
    description  = "created from complaint"
    priority     = "MEDIUM"
    status       = "TO_DO"
    departmentId = 1
    adminId      = 1
} | ConvertTo-Json

$converted = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/complaints/$id/create-task" -Method Post -Body $taskPayload -ContentType "application/json" -Headers @{ "Authorization" = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiQURNSU4iLCJpZCI6NDgsInN1YiI6ImFoZXJjaGV0YW5hMkBnbWFpbC5jb20iLCJpYXQiOjE3Njg3MTQyNjQsImV4cCI6MTc2ODgwMDY2NH0.L9as7LzN22zZsfzTXgiJ77VJ0GThHpU6LWJk68jIG60" }

Write-Host "Converted Task ID: $($converted.relatedTaskId)"
if ($converted.status -eq "CONVERTED_TO_TASK") {
    Write-Host "SUCCESS: Status Updated." -ForegroundColor Green
}
else {
    Write-Host "FAILURE: Status Mismatch: $($converted.status)" -ForegroundColor Red
}
