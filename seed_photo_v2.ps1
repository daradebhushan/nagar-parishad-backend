$loginParams = @{email = "admin@nagarparishad.in"; password = "password" }
$auth = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body ($loginParams | ConvertTo-Json) -ContentType "application/json" -ErrorAction Stop
$token = $auth.data.accessToken

# Create a small dummy image
$dummyImgPath = "$PWD\test_image.jpg"
$bitmap = New-Object System.Drawing.Bitmap(100, 100)
$g = [System.Drawing.Graphics]::FromImage($bitmap)
$g.Clear([System.Drawing.Color]::Red)
$bitmap.Save($dummyImgPath, [System.Drawing.Imaging.ImageFormat]::Jpeg)
$bitmap.Dispose()
$g.Dispose()

Write-Host "Created test image at $dummyImgPath"

# Upload
$uri = "http://localhost:8080/api/admin/complaints/1/attachments"
$authHeader = "Authorization: Bearer $token"

# Use curl.exe with -F
& curl.exe -v -X POST $uri -H $authHeader -F "file=@$dummyImgPath;type=image/jpeg"

Write-Host "Upload Attempt Complete"
