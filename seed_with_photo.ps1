$loginParams = @{email = "admin@nagarparishad.in"; password = "password" }
try {
    $auth = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body ($loginParams | ConvertTo-Json) -ContentType "application/json" -ErrorAction Stop
    $token = $auth.data.token
    Write-Host "Got Token: $token"
}
catch {
    Write-Error "Login Failed: $_"
    exit 1
}

$headers = @{
    "Authorization" = "Bearer $token"
}

# 1. Get List of Complaints
$complaints = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/complaints" -Method Get -Headers $headers
$id = $complaints[0].id
Write-Host "Targeting Complaint ID: $id"

# 2. Upload Attachment
$filePath = "d:\anti gravity\mobile\1_dashboard.png"
$uri = "http://localhost:8080/api/admin/complaints/$id/attachments"

Write-Host "Uploading $filePath to $uri"
# PowerShell older versions struggle with multipart/form-data. Using curl.exe if available or simple .NET
# Using curl.exe for reliability with multipart
$authHeader = "Authorization: Bearer $token"
$fileParam = "file=@$filePath"

# Ensure curl.exe is used (not alias)
& curl.exe -v -X POST $uri -H $authHeader -H "ngrok-skip-browser-warning: true" -F $fileParam

Write-Host "Upload command executed."
