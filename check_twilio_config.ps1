# check_twilio_config.ps1
$url = "https://repayable-kenisha-inapprehensively.ngrok-free.dev/api"
$adminCreds = @{ email = "daradebhushan15+admin@gmail.com"; password = "Bbd@1415" }

# Login
$login = Invoke-RestMethod -Uri "$url/auth/login" -Method Post -Body ($adminCreds | ConvertTo-Json) -ContentType "application/json"
$token = $login.data.token
$headers = @{ Authorization = "Bearer $token" }

Write-Host "Setting up Twilio Config..."
try {
    # Using the setup-config endpoint found in controller
    $headers.Add("ngrok-skip-browser-warning", "true")
    # Need to verify if $headers map handles duplicate keys if I run this multiple times?
    # Powershell hashtable .Add throws if key exists.
    # Better to just set it.
    $headers["ngrok-skip-browser-warning"] = "true"

    $res = Invoke-RestMethod -Uri "$url/public/whatsapp/setup-config" -Method Get -Headers $headers
    Write-Host "Setup Result: $res"
}
catch {
    Write-Host "Check Failed: $_"
}

# Also try setup-config if needed, but let's check first.
