# test_login_v2.ps1
$url = "https://repayable-kenisha-inapprehensively.ngrok-free.dev/api/auth/login"
$body = @{
    email    = "daradebhushan15+admin@gmail.com"
    password = "Bbd@1415"
} | ConvertTo-Json

try {
    $res = Invoke-RestMethod -Uri $url -Method Post -Body $body -ContentType "application/json" -ErrorAction Stop
    Write-Host "Login SUCCESS! Token received."
    Write-Host "Token: $($res.data.token.Substring(0, 10))..."
}
catch {
    Write-Host "Login FAILED. $_"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        Write-Host "Response Body: $($reader.ReadToEnd())"
    }
}
