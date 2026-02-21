
$hostsPath = "$env:SystemRoot\System32\drivers\etc\hosts"
$domain = "loknagar.in"
$entry = "127.0.0.1 $domain"

if (Select-String -Path $hostsPath -Pattern $domain) {
    Write-Host "Domain '$domain' already exists in hosts file."
}
else {
    try {
        Add-Content -Path $hostsPath -Value $entry -ErrorAction Stop
        Write-Host "Successfully added '$entry' to hosts file."
    }
    catch {
        Write-Error "Failed to add entry. Please run as Administrator."
    }
}
