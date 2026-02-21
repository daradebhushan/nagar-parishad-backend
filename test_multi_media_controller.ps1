$url = "http://localhost:8080/api/public/whatsapp"
$body = "From=whatsapp%3A%2B919999999999&To=whatsapp%3A%2B14155238886&Body=Next&NumMedia=2&MediaUrl0=file%3A%2F%2F%2Fd%3A%2Fanti%20gravity%2Fbackend%2Fseed_photo_v2.ps1&MediaUrl1=file%3A%2F%2F%2Fd%3A%2Fanti%20gravity%2Fbackend%2Fseed_photo_v2.ps1"

# We send "Next" because we assume we are at the "ask_photo" stage.
# Or we can send "Hi" then navigate to photo stage. 
# But let's just assume we are in the flow or force it?
# The controller just passes data to service. 
# We need to be in "ask_photo" stage for the service to accept photos.
# Step 1: Reset to Start
Invoke-WebRequest -Uri $url -Method Post -Body "From=whatsapp%3A%2B919999999999&To=whatsapp%3A%2B14155238886&Body=Hi" -ErrorAction Stop
Start-Sleep -Milliseconds 500

# Step 2: Select Lang (1)
Invoke-WebRequest -Uri $url -Method Post -Body "From=whatsapp%3A%2B919999999999&To=whatsapp%3A%2B14155238886&Body=1" -ErrorAction Stop
Start-Sleep -Milliseconds 500

# Step 3: Dept 1
Invoke-WebRequest -Uri $url -Method Post -Body "From=whatsapp%3A%2B919999999999&To=whatsapp%3A%2B14155238886&Body=1" -ErrorAction Stop
Start-Sleep -Milliseconds 500

# Step 4: Name
Invoke-WebRequest -Uri $url -Method Post -Body "From=whatsapp%3A%2B919999999999&To=whatsapp%3A%2B14155238886&Body=Tester" -ErrorAction Stop
Start-Sleep -Milliseconds 500

# Step 5: Desc
Invoke-WebRequest -Uri $url -Method Post -Body "From=whatsapp%3A%2B919999999999&To=whatsapp%3A%2B14155238886&Body=MultiMediaTest" -ErrorAction Stop
Start-Sleep -Milliseconds 500

# Step 6: MULTI MEDIA UPLOAD
Write-Host "Sending 2 Photos..."
try {
    Invoke-WebRequest -Uri $url -Method Post -Body $body -ErrorAction Stop
    Write-Host "Multi-Media Request Sent Successfully"
}
catch {
    Write-Host "Error: $_"
}
