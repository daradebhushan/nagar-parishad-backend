
# reproduce_multi_attachment.ps1

$Url = "http://localhost:8080/api/public/whatsapp"

# Setup: Ensure we have a valid admin config for sandbox
# Using the "Setup Config" endpoint first
Invoke-RestMethod -Uri "http://localhost:8080/api/public/whatsapp/setup-config" -Method Get

# Simulate Step 1: Hi
$paramsHi = @{
    From = "whatsapp:+919999999999"
    To   = "whatsapp:+14155238886"
    Body = "Hi"
}
Invoke-RestMethod -Uri $Url -Method Post -Body $paramsHi
Start-Sleep -Seconds 1

# Simulate Step 2: Language Selection (1 for English)
$paramsLang = @{
    From = "whatsapp:+919999999999"
    To   = "whatsapp:+14155238886"
    Body = "1"
}
Invoke-RestMethod -Uri $Url -Method Post -Body $paramsLang
Start-Sleep -Seconds 1

# Simulate Step 3: Select "Complaint" (Option 1)
$paramsMenu1 = @{
    From = "whatsapp:+919999999999"
    To   = "whatsapp:+14155238886"
    Body = "1"
}
Invoke-RestMethod -Uri $Url -Method Post -Body $paramsMenu1
Start-Sleep -Seconds 1

# Simulate Step 4: Select "Garbage" (Option 1 - Assuming default flow)
# Note: Adjust if your flow is different. Assuming standard flow: Main Menu -> Complaint -> Garbage -> Description -> Photo -> Location
$paramsMenu2 = @{
    From = "whatsapp:+919999999999"
    To   = "whatsapp:+14155238886"
    Body = "1"
}
Invoke-RestMethod -Uri $Url -Method Post -Body $paramsMenu2
Start-Sleep -Seconds 1

# Simulate Step 5: Description
$paramsDesc = @{
    From = "whatsapp:+919999999999"
    To   = "whatsapp:+14155238886"
    Body = "Test Multi Attachment Complaint"
}
Invoke-RestMethod -Uri $Url -Method Post -Body $paramsDesc
Start-Sleep -Seconds 1

# Simulate Step 6: Photo (Send 2 Photos)
# We need to simulate the parameters Twilio sends for media
$paramsPhoto = @{
    From              = "whatsapp:+919999999999"
    To                = "whatsapp:+14155238886"
    Body              = ""
    NumMedia          = "2"
    MediaUrl0         = "https://placehold.co/600x400.jpg"
    MediaUrl1         = "https://placehold.co/400x600.jpg"
    MediaContentType0 = "image/jpeg"
    MediaContentType1 = "image/jpeg"
}
Invoke-RestMethod -Uri $Url -Method Post -Body $paramsPhoto
Start-Sleep -Seconds 5 # Wait for download

# Simulate Step 7: Next (to finish photo collection)
$paramsNext = @{
    From = "whatsapp:+919999999999"
    To   = "whatsapp:+14155238886"
    Body = "Next"
}
Invoke-RestMethod -Uri $Url -Method Post -Body $paramsNext
Start-Sleep -Seconds 1

# Simulate Step 8: Location
$paramsLoc = @{
    From = "whatsapp:+919999999999"
    To   = "whatsapp:+14155238886"
    Body = "Test Location"
}
Invoke-RestMethod -Uri $Url -Method Post -Body $paramsLoc
Start-Sleep -Seconds 2

# Check the latest complaint
$lastCmp = Invoke-RestMethod -Uri "http://localhost:8080/api/complaints" -Method Get | Select-Object -Last 1

Write-Host "Latest Complaint ID: $($lastCmp.id)"
Write-Host "Photo URL: $($lastCmp.photoUrl)"
Write-Host "Attachments Count: $($lastCmp.attachments.Count)"

if ($lastCmp.attachments.Count -ge 2) {
    Write-Host "SUCCESS: Multiple attachments found." -ForegroundColor Green
    $lastCmp.attachments | Format-Table id, fileName, filePath
}
else {
    Write-Host "FAILURE: Expected at least 2 attachments, found $($lastCmp.attachments.Count)." -ForegroundColor Red
}
