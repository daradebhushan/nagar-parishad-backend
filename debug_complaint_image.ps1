$outfile = "d:/anti gravity/backend/debug_output.txt"
"Starting Debug..." | Out-File -FilePath $outfile -Encoding utf8

$loginUrl = "http://localhost:8080/api/auth/login"
$loginBody = @{
    email    = "daradebhushan15+admin@gmail.com"
    password = "Bbd@1415"
} | ConvertTo-Json

try {
    "Attempting login..." | Out-File -FilePath $outfile -Append
    $loginResponse = Invoke-RestMethod -Uri $loginUrl -Method Post -Body $loginBody -ContentType "application/json"
    
    if ($loginResponse.data -and $loginResponse.data.token) {
        $token = $loginResponse.data.token
        "Login Successful." | Out-File -FilePath $outfile -Append
        
        $headers = @{
            Authorization = "Bearer $token"
        }

        $complaintsUrl = "http://localhost:8080/api/admin/complaints"
        "Fetching complaints..." | Out-File -FilePath $outfile -Append
        $complaints = Invoke-RestMethod -Uri $complaintsUrl -Method Get -Headers $headers

        $target = $complaints | Where-Object { $_.complaintNo -eq "CMP-35-1361" }

        if ($target) {
            "Found Complaint: $($target.complaintNo)" | Out-File -FilePath $outfile -Append
            "ID: $($target.id)" | Out-File -FilePath $outfile -Append
            "PhotoURL: $($target.photoUrl)" | Out-File -FilePath $outfile -Append
            "Attachments:" | Out-File -FilePath $outfile -Append
            $target.attachments | ForEach-Object {
                "  - ID: $($_.id)" | Out-File -FilePath $outfile -Append
                "    FileName: $($_.fileName)" | Out-File -FilePath $outfile -Append
                "    Url: $($_.url)" | Out-File -FilePath $outfile -Append
            }
        }
        else {
            "Complaint CMP-42-4837 not found." | Out-File -FilePath $outfile -Append
        }
    }
    else {
        "Login failed. No token." | Out-File -FilePath $outfile -Append
    }

}
catch {
    "Error: $_" | Out-File -FilePath $outfile -Append
}
