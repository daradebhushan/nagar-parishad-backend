$domain = "http://localhost:8080"
$body = '{"email":"daradebhushan15+admin@gmail.com", "password":"Bbd@123"}'
$authResponse = Invoke-RestMethod -Uri "$domain/api/auth/login" -Method Post -Body $body -ContentType "application/json"
$token = $authResponse.data.token

# Create Dept
$newDeptBody = '{ "name": "E2E Roles Dept", "nameMr": "E2E भूमिका विभाग" }'
$deptResponse = Invoke-RestMethod -Uri "$domain/api/admin/departments" -Method Post -Body $newDeptBody -ContentType "application/json" -Headers @{Authorization="Bearer $token"}
$deptId = $deptResponse.id

# Create Dept Head
$newHeadBody = @{
    name = "E2E Head User"
    email = "head_test@loknagar.com"
    mobile = "8888888888"
    password = "Password@123"
    role = "DEPARTMENT_HEAD"
    departmentId = $deptId
} | ConvertTo-Json

Invoke-RestMethod -Uri "$domain/api/admin/users" -Method Post -Body $newHeadBody -ContentType "application/json" -Headers @{Authorization="Bearer $token"}

# Create Staff
$newStaffBody = @{
    name = "E2E Staff User"
    email = "staff_test@loknagar.com"
    mobile = "7777777777"
    password = "Password@123"
    role = "STAFF"
    departmentId = $deptId
} | ConvertTo-Json

Invoke-RestMethod -Uri "$domain/api/admin/users" -Method Post -Body $newStaffBody -ContentType "application/json" -Headers @{Authorization="Bearer $token"}

Write-Output "Created Dept $deptId and Users"
