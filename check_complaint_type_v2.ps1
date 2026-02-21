$loginUrl = "http://localhost:8080/api/auth/login"
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
    
    Write-Host "Complaint Types Raw Data:"
    Write-Host ($types.data | ConvertTo-Json -Depth 5)

}
catch {
    Write-Host "Error: $_"
}
