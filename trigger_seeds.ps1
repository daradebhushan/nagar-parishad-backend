# trigger_seeds.ps1
$baseUrl = "https://repayable-kenisha-inapprehensively.ngrok-free.dev/api"
$ownerCreds = @{ email = "daradebhushan15+admin@gmail.com"; password = "Bbd@1415" }
# Note: Variable name is ownerCreds but we are putting Admin creds to seed for Admin.

# Login
$login = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body ($ownerCreds | ConvertTo-Json) -ContentType "application/json"
$token = $login.data.token
$headers = @{ Authorization = "Bearer $token" }

Write-Host "Seeding Departments..."
try {
    Invoke-RestMethod -Uri "$baseUrl/admin/departments/seed-defaults" -Method Post -Headers $headers
    Write-Host "Departments Seeded."
}
catch { Write-Host "Dept Seed Failed: $_" }

Write-Host "Seeding Complaint Types..."
try {
    Invoke-RestMethod -Uri "$baseUrl/admin/seeder/seed-types" -Method Post -Headers $headers
    Write-Host "Types Seeded."
}
catch { Write-Host "Type Seed Failed: $_" }

# Check for Designation Seed (if exists, add here, otherwise I will manual create)
# Inspecting DesignationController next...
