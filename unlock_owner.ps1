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
        
        # 2. Get All Users to find Owner
        Write-Host "Fetching users to find Owner..."
        $usersResponse = Invoke-RestMethod -Uri $usersUrl -Method Get -Headers $headers
        $users = $usersResponse.data.content
        
        $owner = $users | Where-Object { $_.email -eq "owner@govt.in" }
        
        if ($owner) {
            Write-Host "Found Owner. ID: $($owner.id), Active: $($owner.active)"
            
            # 3. Update Active Status
            $updatePayload = @{
                name   = $owner.name
                email  = $owner.email
                mobile = $owner.mobile
                role   = $owner.role
                active = $true
            }
            $updateBody = $updatePayload | ConvertTo-Json
            $updateUrl = "$updateUrlBase$($owner.id)"
            
            Write-Host "Unlocking Owner Account..."
            $updateResponse = Invoke-RestMethod -Uri $updateUrl -Method Put -Body $updateBody -Headers $headers -ContentType "application/json"
            
            if ($updateResponse.success -eq $true) {
                Write-Host "Owner Account Unlocked Successfuly."
            }
            else {
                Write-Error "Unlock Failed: $($updateResponse.message)"
            }
            
        }
        else {
            Write-Error "User 'owner@govt.in' not found in user list."
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
