$loginUrl = "http://localhost:8080/api/auth/login"
$typeUrl = "http://localhost:8080/api/admin/complaint-types/1" 
# Assuming endpoint exists, otherwise list all
$listUrl = "http://localhost:8080/api/admin/complaint-types"

$loginPayload = @{
    email    = "admin@nagarparishad.in"
    password = "password"
} | ConvertTo-Json

try {
    Write-Host "Logging in..."
    $response = Invoke-RestMethod -Uri $loginUrl -Method Post -Body $loginPayload -ContentType "application/json"
    $token = $response.data.token
    $headers = @{ Authorization = "Bearer $token" }
    
    Write-Host "Fetching Complaint Types..."
    $types = Invoke-RestMethod -Uri $listUrl -Method Get -Headers $headers
    
    Write-Host "Complaint Types Found:"
    $types.data | Format-Table id, nameEn, nameMr, active
    
    $type1 = $types.data | Where-Object { $_.id -eq 1 }
    if ($type1) {
        Write-Host "ID 1 Details: $($type1 | ConvertTo-Json)"
    }
    else {
        Write-Host "ID 1 NOT FOUND."
    }

}
catch {
    Write-Host "Error: $_"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        Write-Host "Response: $($reader.ReadToEnd())"
    }
}
