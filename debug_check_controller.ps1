try {
    $url = "http://localhost:8080/api/public/whatsapp/check-config" # Using this because I need a valid endpoint to hijack? No wait, I can't query DB via API unless I expose it.
    # Actually, I should just use the same "Temporary Endpoint" trick to dump session info.
    # The user can't wait for another restart.
    
    # Alternative: Use the "Simulate" method logic which printed debug info... but I reversed that.
    # The log already showed: "Duplicate entry ... for key 'chatbot_sessions.UK_...'"
    
    # I will try to DELETE the session via a temporary endpoint (since restart takes too long, I can't do that).
    # Wait, I can't add an endpoint without restart.
    
    # I must use the EXISTING code.
    # Does 'simulate' still exist? Yes.
    # Can I trigger 'simulate' via API? No, it's internal.
    
    # START TRICK:
    # I already added /check-config and /setup-config. I verified they work.
    # I CANNOT change Java code effective immediately without restart.
    
    # Option: Manually delete the row? I don't have SQL access.
    # Option: Use the 'Reset' logic?
    # If I send "RESET", it goes to 'processMessage'.
    # 'processMessage' fails at STEP 2 (Session Lookup/Create) BEFORE processing the message content.
    # So sending "RESET" won't help because it crashes before reading the message.
    
    # CRITIAL OBSERVATION:
    # The Log says: "Duplicate entry '+918237930576'".
    # Only ONE format.
    # This implies there is ALREADY a session with this number.
    # BUT `sessionRepository.findByMobileNumberAndAdminId(mobile, admin.getId())` returned Empty.
    # Admin ID is 2.
    # So, there is a session with `mobile_number = '+918237930576'` BUT `admin_id != 2`.
    # It must belong to Admin 1.
    
    # AND the constraint is Global on Mobile Number (incorrectly).
    
    # HYPOTHESIS CONFIRMED:
    # 1. Existing Session: Mobile=+91... Admin=1
    # 2. New Request: Mobile=+91... Admin=2
    # 3. Find(Mobile, 2) -> Empty.
    # 4. Save(Mobile, 2) -> Fails because Unique Constraint is on Mobile ONLY (Legacy Schema).
    
    # FIX:
    # I need to fix the schema OR remove the old session.
    # Since I cannot run SQL, and I cannot restart to run schema fixers (which seemingly failed or skipped?),
    # I must use code to delete the conflicting session.
    # BUT I can't run code without restart.
    
    # WAIT. I can use the `check-config` endpoint I just added?
    # No, it's hardcoded to return config.
    
    # DID I REVERT THE `check-config` endpoint?
    # Let's check `WhatsappController.java`.
    
    Write-Host "Checking Controller..."
    Get-Content "d:\anti gravity\backend\src\main\java\com\nagar\parishad\backend\controller\WhatsappController.java"
    
}
catch {
    Write-Error "Error: $_"
}
