$ErrorActionPreference = "Stop"

# 1. Login
$loginUrl = "http://localhost:8080/api/auth/login"
$loginBody = @{
    email    = "admin@test.com"
    password = "password"
} | ConvertTo-Json

Write-Host "Logging in..."
try {
    $loginResponse = Invoke-RestMethod -Uri $loginUrl -Method Post -Body $loginBody -ContentType "application/json"
}
catch {
    Write-Error "Login failed: $_"
    exit 1
}

$token = $loginResponse.data.token
if (-not $token) {
    # Try alternate: just in case
    $token = $loginResponse.token
}

$headers = @{
    Authorization = "Bearer $token"
}

# 2. Get All Tasks to find a valid ID
Write-Host "`nFetching All Tasks..."
try {
    $listResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/tasks" -Method Get -Headers $headers
    $tasks = $listResponse.data.content
    if ($tasks.Count -eq 0) {
        Write-Error "No tasks found in the system."
        exit 1
    }
    $validRoleId = $tasks[0].id
    Write-Host "Found Task ID: $validRoleId"
}
catch {
    Write-Error "Get All Tasks failed: $_"
    try { Write-Host "Error Body: " ($_.Exception.Response.GetResponseStream() | ForEach-Object { (New-Object System.IO.StreamReader $_).ReadToEnd() }) } catch {}
    exit 1
}

# 3. Get Task Detail
Write-Host "`nFetching Task Detail for ID: $validRoleId..."
try {
    $taskResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/tasks/$validRoleId" -Method Get -Headers $headers
    Write-Host "Task Response: " ($taskResponse | ConvertTo-Json -Depth 5)
}
catch {
    Write-Error "Get Task Detail failed: $_"
    try { Write-Host "Error Body: " ($_.Exception.Response.GetResponseStream() | ForEach-Object { (New-Object System.IO.StreamReader $_).ReadToEnd() }) } catch {}
}

# 4. Add Comment
Write-Host "`nAdding Comment to Task ID: $validRoleId..."
$commentBody = @{
    text = "Test Comment from Debug Script"
} | ConvertTo-Json

try {
    $addCommentResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/tasks/$validRoleId/comments" -Method Post -Headers $headers -Body $commentBody -ContentType "application/json"
    Write-Host "Add Comment Response: " ($addCommentResponse | ConvertTo-Json -Depth 5)
}
catch {
    Write-Error "Add Comment failed: $_"
    try { Write-Host "Error Body: " ($_.Exception.Response.GetResponseStream() | ForEach-Object { (New-Object System.IO.StreamReader $_).ReadToEnd() }) } catch {}
}

# 5. Get Comments
Write-Host "`nFetching Comments for Task ID: $validRoleId..."
try {
    $commentsResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/tasks/$validRoleId/comments" -Method Get -Headers $headers
    Write-Host "Comments Response: " ($commentsResponse | ConvertTo-Json -Depth 5)
}
catch {
    Write-Error "Get Comments failed: $_"
    try { Write-Host "Error Body: " ($_.Exception.Response.GetResponseStream() | ForEach-Object { (New-Object System.IO.StreamReader $_).ReadToEnd() }) } catch {}
}
