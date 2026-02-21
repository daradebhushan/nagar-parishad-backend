$ErrorActionPreference = "Stop"

$baseUrl = "http://localhost:8080/api"
$adminEmail = "admin@nagarparishad.in"
$password = "password"

# 1. Login
Write-Host "Logging in..."
$loginBody = @{
    email = $adminEmail
    password = $password
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
    $token = $loginResponse.data.token
    Write-Host "Login Successful. Token received."
} catch {
    Write-Error "Login Failed: $_"
}

$headers = @{
    Authorization = "Bearer $token"
}

# 2. Create Task
Write-Host "Creating Task..."
# Assign to ID 4 (Ramesh Staff from Seed Data)
$currentDate = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss")
$taskBody = @{
    title = "Test Task with Attachment $currentDate"
    description = "This is a test task to verify email attachments."
    priority = "HIGH"
    status = "PENDING"
    type = "General"
    departmentId = 1 # Sanitation
    assignedStaffId = 4 # Ramesh Staff
} | ConvertTo-Json

try {
    $createResponse = Invoke-RestMethod -Uri "$baseUrl/tasks/create" -Method Post -Body $taskBody -ContentType "application/json" -Headers $headers
    $taskId = $createResponse.data.id
    Write-Host "Task Created. ID: $taskId"
} catch {
    Write-Error "Task Creation Failed: $_"
}

# 3. Upload Attachment
Write-Host "Uploading Attachment..."
$imagePath = "C:\Users\darad\.gemini\antigravity\brain\dc714c5b-cff1-434b-aa46-e802f3f907be\test_attachment_1767536465178.png"

# Manual Multipart/Form-Data construction because Invoke-RestMethod -Form is tricky with files in some PS versions
# Using curl for reliability if available, or .NET HttpClient. 
# Let's try simple .NET HttpClient in PS for file upload.

Add-Type -AssemblyName System.Net.Http
$client = New-Object System.Net.Http.HttpClient
$client.DefaultRequestHeaders.Add("Authorization", "Bearer $token")

$content = New-Object System.Net.Http.MultipartFormDataContent
$fileStream = [System.IO.File]::OpenRead($imagePath)
$fileContent = New-Object System.Net.Http.StreamContent($fileStream)
$fileContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse("image/png")
$content.Add($fileContent, "file", "test_attachment.png")

$uploadUri = "$baseUrl/tasks/$taskId/attachments"
$response = $client.PostAsync($uploadUri, $content).Result

if ($response.IsSuccessStatusCode) {
    Write-Host "Attachment Uploaded Successfully."
} else {
    Write-Error "Attachment Upload Failed: $($response.StatusCode)"
}
$fileStream.Close()

# 4. Notify
Write-Host "Triggering Notification..."
try {
    $notifyResponse = Invoke-RestMethod -Uri "$baseUrl/tasks/$taskId/notify" -Method Post -ContentType "application/json" -Headers $headers
    Write-Host "Notification Triggered: $($notifyResponse.message)"
} catch {
    Write-Error "Notification Failed: $_"
}
