# diagnose_data.ps1
$baseUrl = "https://repayable-kenisha-inapprehensively.ngrok-free.dev/api"
$ownerCreds = @{ email = "daradebhushan15+owner@gmail.com"; password = "Bbd@1415" }
$adminCreds = @{ email = "daradebhushan15+admin@gmail.com"; password = "Bbd@1415" }

function Get-DataStats ($token, $roleName) {
    $headers = @{ Authorization = "Bearer $token" }
    
    Write-Host "--- Stats for $roleName ---"
    
    # Users
    try {
        $uRes = Invoke-RestMethod -Uri "$baseUrl/admin/users?page=0&size=10" -Method Get -Headers $headers -ErrorAction Stop
        $uCount = if ($uRes.data.content) { $uRes.data.content.Count } else { $uRes.data.Count }
        $totalUsers = if ($uRes.data.totalElements) { $uRes.data.totalElements } else { "N/A" }
        Write-Host "Visible Users: $uCount (Total: $totalUsers)"
    }
    catch { Write-Host "Users: Error $_" }

    # Complaints (Assuming endpoint)
    try {
        $cRes = Invoke-RestMethod -Uri "$baseUrl/complaints?page=0&size=10" -Method Get -Headers $headers -ErrorAction Stop
        $cCount = if ($cRes.data.content) { $cRes.data.content.Count } else { $cRes.data.Count }
        $totalComp = if ($cRes.data.totalElements) { $cRes.data.totalElements } else { "N/A" }
        Write-Host "Visible Complaints: $cCount (Total: $totalComp)"
    }
    catch { Write-Host "Complaints: Error $_" }
    
    Write-Host ""
}

# 1. Check Owner
Write-Host "Logging in as OWNER..."
try {
    $login = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body ($ownerCreds | ConvertTo-Json) -ContentType "application/json"
    Get-DataStats $login.data.token "OWNER"
}
catch { Write-Host "Owner Login Failed: $_" }

# 2. Check Admin
Write-Host "Logging in as ADMIN..."
try {
    $login = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body ($adminCreds | ConvertTo-Json) -ContentType "application/json"
    Get-DataStats $login.data.token "ADMIN"
}
catch { Write-Host "Admin Login Failed: $_" }
