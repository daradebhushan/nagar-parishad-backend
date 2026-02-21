$Headers = @{
    "Authorization" = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiQURNSU4iLCJpZCI6OCwic3ViIjoiYWRtaW5AdGVzdC5jb20iLCJpYXQiOjE3NjcxNjMxOTAsImV4cCI6MTc2NzI0OTU5MH0.F3wfrBm1a3EH559U8dwFXEJQaluIvv655fbJgdzkboA"
}
$ContentType = "application/json"

Write-Host "0. Checking Token Validity..."
try {
    $task = Invoke-RestMethod -Uri "http://localhost:8080/api/tasks/67" -Method Get -Headers $Headers
    Write-Host "Token Valid. Task ID: $($task.data.id)" -ForegroundColor Green
}
catch {
    Write-Host "Token Invalid or Backend Down: $($_.Exception.Message)" -ForegroundColor Red
    exit
}

Write-Host "1. Getting Comments for Task 67..."
$comments = Invoke-RestMethod -Uri "http://localhost:8080/api/tasks/67/comments" -Method Get -Headers $Headers
if ($comments.data.Count -eq 0) {
    Write-Host "No comments found. Creating one..."
    $newComment = Invoke-RestMethod -Uri "http://localhost:8080/api/tasks/67/comments" -Method Post -Headers $Headers -ContentType $ContentType -Body '{"text": "Temp Comment for CRUD"}'
    $commentId = $newComment.data.id
}
else {
    $commentId = $comments.data[0].id
}
Write-Host "Target Comment ID: $commentId"

Write-Host "2. Updating Comment $commentId..."
$updated = Invoke-RestMethod -Uri "http://localhost:8080/api/tasks/67/comments/$commentId" -Method Put -Headers $Headers -ContentType $ContentType -Body '{"text": "API UPDATED COMMENT"}'
Write-Host "Update Response: $($updated.data.text)"

Write-Host "3. Verifying Update..."
$verify = Invoke-RestMethod -Uri "http://localhost:8080/api/tasks/67/comments" -Method Get -Headers $Headers
$match = $verify.data | Where-Object { $_.id -eq $commentId }
if ($match.text -eq "API UPDATED COMMENT") { Write-Host "SUCCESS: Comment Updated" -ForegroundColor Green } else { Write-Host "FAIL: Comment Not Updated" -ForegroundColor Red }

Write-Host "4. Deleting Comment $commentId..."
Invoke-RestMethod -Uri "http://localhost:8080/api/tasks/67/comments/$commentId" -Method Delete -Headers $Headers
Write-Host "Delete Request Sent"

Write-Host "5. Verifying Deletion..."
$final = Invoke-RestMethod -Uri "http://localhost:8080/api/tasks/67/comments" -Method Get -Headers $Headers
$stillExists = $final.data | Where-Object { $_.id -eq $commentId }
if (-not $stillExists) { Write-Host "SUCCESS: Comment Deleted" -ForegroundColor Green } else { Write-Host "FAIL: Comment Still Exists" -ForegroundColor Red }
