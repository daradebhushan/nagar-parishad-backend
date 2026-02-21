$url = "http://localhost:8080/api/public/whatsapp"
$body = @{
    From     = "whatsapp:+919999999999"
    To       = "whatsapp:+14155238886"
    Body     = "Test Webhook"
    NumMedia = "0"
}

try {
    # Simulate Form-UrlEncoded which Twilio uses
    Invoke-WebRequest -Uri $url -Method Post -Body $body -ErrorAction Stop
    Write-Host "Webhook Hit Successful (200 OK)"
}
catch {
    Write-Host "Webhook Failed: $_"
}
