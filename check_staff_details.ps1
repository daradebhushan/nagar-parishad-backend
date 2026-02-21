# check_staff_details.ps1
$baseUrl = "https://repayable-kenisha-inapprehensively.ngrok-free.dev/api"
$ownerCreds = @{ email = "daradebhushan15+owner@gmail.com"; password = "Bbd@1415" }

Write-Host "Logging in as Owner..."
$login = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body ($ownerCreds | ConvertTo-Json) -ContentType "application/json"
$token = $login.data.token
$headers = @{ Authorization = "Bearer $token" }

Write-Host "Searching for staff@demo.com..."
# Owner sees ALL users
$usersRes = Invoke-RestMethod -Uri "$baseUrl/admin/users?page=0&size=100" -Method Get -Headers $headers
$users = if ($usersRes.data.content) { $usersRes.data.content } else { $usersRes.data }
$staff = $users | Where-Object { $_.email -eq "staff@demo.com" }

if ($staff) {
    Write-Host "Staff Found: $($staff.name) (ID: $($staff.id))"
    Write-Host "Role: $($staff.role)"
    
    if ($staff.admin) {
        Write-Host "Admin Assigned: Yes"
        Write-Host "Admin ID: $($staff.admin.id)"
        Write-Host "Admin Name: $($staff.admin.name)"
    }
    else {
        Write-Host "Admin Assigned: NO (Field is null)"
    }
}
else {
    Write-Host "Staff User NOT FOUND."
}
