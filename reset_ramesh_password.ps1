$loginUrl = "http://localhost:8080/api/auth/login"
$usersUrl = "http://localhost:8080/api/admin/users?page=0&size=100"
$updateUrlBase = "http://localhost:8080/api/admin/users/"

# 1. Login as Admin
$adminCreds = @{
    email    = "admin@nagarparishad.in"
    password = "password"
}
$body = $adminCreds | ConvertTo-Json

try {
    Write-Host "Logging in as Admin..."
    $loginResponse = Invoke-RestMethod -Uri $loginUrl -Method Post -Body $body -ContentType "application/json"
    
    if ($loginResponse.success -eq $true) {
        $token = $loginResponse.data.token
        Write-Host "Login Successful. Token received."
        
        $headers = @{
            Authorization = "Bearer $token"
        }
        
        # 2. Get All Users to find Ramesh
        Write-Host "Fetching users..."
        $usersResponse = Invoke-RestMethod -Uri $usersUrl -Method Get -Headers $headers
        $users = $usersResponse.data.content
        
        $ramesh = $users | Where-Object { $_.email -eq "ramesh@ghantagadi.in" }
        
        if ($ramesh) {
            Write-Host "Found Ramesh. ID: $($ramesh.id)"
            
            # 3. Update Password
            $updatePayload = @{
                name     = $ramesh.name
                email    = $ramesh.email
                mobile   = $ramesh.mobile
                password = "password"
                role     = $ramesh.role
                active   = $true
            }
            $updateBody = $updatePayload | ConvertTo-Json
            $updateUrl = "$updateUrlBase$($ramesh.id)"
            
            Write-Host "Resetting password for Ramesh..."
            $updateResponse = Invoke-RestMethod -Uri $updateUrl -Method Put -Body $updateBody -Headers $headers -ContentType "application/json"
            
            if ($updateResponse.success -eq $true) {
                Write-Host "Password Reset Successful for Ramesh."
            }
            else {
                Write-Error "Password Reset Failed: $($updateResponse.message)"
            }
            
        }
        else {
            Write-Error "User 'ramesh@ghantagadi.in' not found in user list."
        }
    }
    else {
        Write-Error "Login Failed: $($loginResponse.message)"
    }
}
catch {
    Write-Error "Error: $_"
    Write-Host "Exception Details: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        Write-Host "Response Body: $($reader.ReadToEnd())"
    }
}
