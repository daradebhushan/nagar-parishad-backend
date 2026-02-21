$ErrorActionPreference = "Stop"

# ... (Previous login code omitted for brevity if unchanged, but I'll include full for safety or just the update)
# I'll rewrite the whole file to be safe and complete.

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
    Write-Error "No token received. Response data: $($loginResponse.data | ConvertTo-Json -Depth 2)"
    exit 1
}
Write-Host "Login successful. Token received."

# 2. Create Task
$createTaskUrl = "http://localhost:8080/api/tasks/create"
$headers = @{
    Authorization = "Bearer $token"
}

# Ensure Date format is correct for default Spring Boot (often expects ISO-8601)
$taskBody = @{
    title       = "Verification Task with Attachment"
    description = "Task created by verification script to test file upload"
    priority    = "HIGH"
    status      = "TO_DO"  # Corrected Enum Value
    dueDate     = (Get-Date).AddDays(5).ToString("yyyy-MM-ddTHH:mm:ss")
} | ConvertTo-Json

Write-Host "Creating Task with body: $taskBody"
try {
    $taskResponse = Invoke-RestMethod -Uri $createTaskUrl -Method Post -Headers $headers -Body $taskBody -ContentType "application/json"
}
catch {
    # Enhanced Error Handling
    $result = $_.Exception.Response.GetResponseStream()
    $reader = New-Object System.IO.StreamReader($result)
    $responseBody = $reader.ReadToEnd()
    Write-Error "Create Task failed. Status: $($_.Exception.Response.StatusCode)`nBody: $responseBody"
    exit 1
}

$taskId = $taskResponse.data.id
Write-Host "Task created. ID: $taskId"

# 3. Upload Attachment
$uploadUrl = "http://localhost:8080/api/tasks/$taskId/attachments"
$filePath = "C:\Users\b\Downloads\VIP TOUR EXPENDITURE March 2025 (1).pdf"

if (-not (Test-Path $filePath)) {
    Write-Warning "File not found: $filePath"
    Write-Warning "Creating a dummy file for verification purposes..."
    $filePath = "d:\anti gravity\test_attachment.pdf"
    "Dummy PDF content" | Set-Content $filePath
}

Write-Host "Uploading attachment from $filePath..."
$curlCmd = "curl.exe -X POST ""$uploadUrl"" -H ""Authorization: Bearer $token"" -F ""file=@$filePath"""
Write-Host "Executing: $curlCmd"
# Invoke-Expression is tricky with quotes. Better to use Start-Process or just call it directly if possible.
# Simple way:
cmd /c $curlCmd
Write-Host "`nUpload attempt completed."
