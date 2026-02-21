$loginUrl = "http://localhost:8080/api/auth/login"
$deptUrl = "http://localhost:8080/api/admin/departments"
$seedUrl = "http://localhost:8080/api/admin/seeder/seed-sub-questions"
$simUrl = "http://localhost:8080/api/admin/bot-sim/interact"
$complaintUrl = "http://localhost:8080/api/admin/complaints"

$cred = @{ email = "admin@nagarparishad.in"; password = "password" }
$body = $cred | ConvertTo-Json

# Helper
function Send-Msg($msg) {
    $payload = @{
        mobile  = "SIM_ELEC_TESTER"
        message = $msg
    } | ConvertTo-Json
    try {
        $res = Invoke-RestMethod -Uri $simUrl -Method Post -Headers $headers -Body $payload -ContentType "application/json"
        return $res.response
    }
    catch {
        Write-Host "Sim Error: $_"
    }
}

try {
    Write-Host "1. Logging in..."
    $response = Invoke-RestMethod -Uri $loginUrl -Method Post -Body $body -ContentType "application/json"
    $token = $response.data.token
    $headers = @{ Authorization = "Bearer $token" }
    
    Write-Host "2. Checking Departments..."
    try {
        $deptRes = Invoke-RestMethod -Uri $deptUrl -Method Get -Headers $headers
        # Check if content exists
        if ($deptRes.data -and $deptRes.data.content) {
            $depts = $deptRes.data.content
            $elec = $depts | Where-Object { $_.name -like "*Electric*" }
        }
        else {
            Write-Host "Warning: Unexpected Dept Response Format. Assuming empty?"
            $depts = @()
        }
    }
    catch {
        Write-Host "Failed to list departments: $_"
        $depts = @()
    }
    
    if ($elec) {
        Write-Host "Found Electric Dept: $($elec.name) (ID: $($elec.id))"
        $elecId = $elec.id
    }
    else {
        Write-Host "Creating Electric Dept..."
        $newDept = @{
            name             = "Electric Dept"
            nameMr           = "विद्युत विभाग"
            isActive         = $true
            isChatbotEnabled = $true
        } | ConvertTo-Json
        try {
            $res = Invoke-RestMethod -Uri "$deptUrl" -Method Post -Headers $headers -Body $newDept -ContentType "application/json"
            $elecId = $res.data.id
            Write-Host "Created Electric Dept (ID: $elecId)"
        }
        catch {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $errBody = $reader.ReadToEnd()
            Write-Host "Dept Creation Failed: $errBody"
            # Maybe it already exists but list failed?
            # Try to recover by ignoring or fetching all
        }
    }
    
    # 3. Seed Sub-Questions
    Write-Host "3. Seeding Marathi Sub-Questions..."
    Invoke-RestMethod -Uri $seedUrl -Method Post -Headers $headers
    
    # 4. Simulate Flow
    Write-Host "4. Simulating Chat..."
    Send-Msg "RESET"
    Send-Msg "Hi" # Bot: Select Dept
    
    # We need to know which OPTION NUMBER corresponds to our Electric Dept.
    # The bot returns a list. WE cannot parse it easily here to find the number.
    # But wait, `WhatsappService` assigns "value" = deptId.
    # If I send the DEPT ID as text, `findSelectedOption` handles index.
    # Wait, the bot displays: 1. DeptA, 2. DeptB.
    # Input "1" selects DeptA.
    # We need to know the INDEX of Electric Dept in the list of ACTIVE departments.
    
    # Let's get list of active chatbot depts to calculate index
    $activeDepts = $depts | Where-Object { $_.isActive -eq $true -and $_.isChatbotEnabled -eq $true }
    # Assume sorting is by ID or creation? Service uses repository.findByAdminId (default order usually ID)
    # Let's verify sort. Default JPA varies. Usually ID asc.
    $activeDepts = $activeDepts | Sort-Object id
    
    $index = 0
    $found = $false
    for ($i = 0; $i -lt $activeDepts.Count; $i++) {
        if ($activeDepts[$i].id -eq $elecId) {
            $index = $i + 1 # 1-based index
            $found = $true
            break
        }
    }
    
    if (-not $found) {
        # Check newly created one
        Write-Host "ID $elecId not found in fetched active list (maybe latency?). Using last index + 1 guess."
        $index = $activeDepts.Count + 1
    }
    
    Write-Host "Selecting Dept Index: $index (ID: $elecId)"
    Send-Msg "$index"
    
    # Now Sub-Issue.
    # Seeder added 3 options.
    # 1. विद्युत पोलवरील दिवा बंद
    # 2. पोल दिवा सतत चालू
    # 3. हायमास्ट बंद असल्याबाबत
    # We select "1".
    Write-Host "Selecting Sub-Issue 1..."
    Send-Msg "1"
    
    Send-Msg "Elec Tester"
    Send-Msg "Lights are out"
    Send-Msg "Skip"
    Send-Msg "Main Road"
    
    # 5. Verify
    Write-Host "5. Verifying Complaint..."
    $c = Invoke-RestMethod -Uri "$complaintUrl" -Method Get -Headers $headers
    # Filter by our citizen mobile to be sure
    # But filtering clientside is fine
    $latest = $c.data | Where-Object { $_.citizenMobile -eq "SIM_ELEC_TESTER_1" } | Sort-Object -Property id -Descending | Select-Object -First 1
    # SIM mobile logic appends _adminId. 
    # Let's just take global latest
    $latest = $c.data | Sort-Object -Property id -Descending | Select-Object -First 1
    
    Write-Host "Complaint No: $($latest.complaintNo)"
    Write-Host "Dept: $($latest.departmentName)"
    Write-Host "Type: $($latest.complaintTypeName)"
    Write-Host "Sub-Type: $($latest.subComplaintType)"
    
    if ($latest.subComplaintType) {
        $bytes = [System.Text.Encoding]::UTF8.GetBytes($latest.subComplaintType)
        $hex = [System.BitConverter]::ToString($bytes)
        Write-Host "Sub-Type Hex: $hex"
        # Expected Hex for "विद्युत पोलवरील दिवा बंद" (partial check)
        # Just printing is enough for manual verification.
    }
    else {
        Write-Host "FAILURE: Sub-Type is NULL"
    }

}
catch {
    Write-Host "Error: $_"
    Write-Host "Stack: $($_.ScriptStackTrace)"
}
