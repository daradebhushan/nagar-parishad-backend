# fetch_json.ps1
$url = "https://repayable-kenisha-inapprehensively.ngrok-free.dev/api"
$adminCreds = @{ email = "daradebhushan15+admin@gmail.com"; password = "Bbd@1415" }

try {
    $login = Invoke-RestMethod -Uri "$url/auth/login" -Method Post -Body ($adminCreds | ConvertTo-Json) -ContentType "application/json"
    $token = $login.data.token
    $headers = @{ Authorization = "Bearer $token" }

    $users = Invoke-RestMethod -Uri "$url/admin/users?page=0&size=10" -Method Get -Headers $headers
    $uCount = if ($users.data.content) { $users.data.content.Count } else { $users.data.Count }
    
    $complaints = Invoke-RestMethod -Uri "$url/admin/complaints" -Method Get -Headers $headers
    $cCount = if ($complaints.data) { $complaints.data.Count } else { $complaints.Count }

    Write-Host "Users Found: $uCount"
    Write-Host "Complaints Found: $cCount"
}
catch {
    Write-Host "Error: $_"
}
