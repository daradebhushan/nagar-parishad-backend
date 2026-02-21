
Write-Host "Seeding Config..."
Invoke-RestMethod -Uri "http://localhost:8080/api/public/diag/seed-config"

Write-Host "Seeding Twilio..."
Invoke-RestMethod -Uri "http://localhost:8080/api/public/diag/seed-twilio"

Write-Host "Cleaning Departments..."
Invoke-RestMethod -Uri "http://localhost:8080/api/public/diag/cleanup-departments"

Write-Host "Seeding Types..."
Invoke-RestMethod -Uri "http://localhost:8080/api/public/diag/seed-types"

Write-Host "Setup Complete."
