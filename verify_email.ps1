$EmailFrom = "emailnotifications26@gmail.com"
$EmailTo = "bhushandarade1407@gmail.com"
$Subject = "Test Email from PowerShell"
$Body = "This is a test email to verify SMTP credentials and connectivity."
$SMTPServer = "smtp.gmail.com"
$SMTPClient = New-Object Net.Mail.SmtpClient($SmtpServer, 587)
$SMTPClient.EnableSsl = $true
$SMTPClient.Credentials = New-Object System.Net.NetworkCredential("emailnotifications26@gmail.com", "gzmk pjle udgz acqt")

try {
    $SMTPClient.Send($EmailFrom, $EmailTo, $Subject, $Body)
    Write-Host "Email sent successfully to $EmailTo"
}
catch {
    Write-Error "Failed to send email: $_"
}
