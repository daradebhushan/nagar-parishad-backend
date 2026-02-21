# simulate_whatsapp.ps1
$url = "https://repayable-kenisha-inapprehensively.ngrok-free.dev/api/public/whatsapp"
$body = "From=whatsapp%3A%2B919876543210&To=whatsapp%3A%2B14155238886&Body=Hi"

Write-Host "Simulating WhatsApp Webhook to $url..."
try {
    # Curl is better for form-urlencoded simulation here or Invoke-WebRequest
    # Removing header to test if Auth Token fixed the warning
    $response = Invoke-WebRequest -Uri $url -Method Post -Body $body -ContentType "application/x-www-form-urlencoded"
    Write-Host "Response Code: $($response.StatusCode)"
    Write-Host "Response Content: $($response.Content)"
}
catch {
    Write-Host "Request Failed: $_"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        Write-Host "Error Body: $($reader.ReadToEnd())"
    }
}
