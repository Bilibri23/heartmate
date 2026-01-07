# PowerShell Script to Create Admin Account
# Run this in PowerShell

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Creating Admin Account for RoomBuddy" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Admin credentials
$adminData = @{
    email = "admin@roombuddy.com"
    password = "Admin123!"
    firstName = "Admin"
    lastName = "User"
    phoneNumber = "+237600000000"
    role = "ADMIN"
} | ConvertTo-Json

Write-Host "Admin Credentials:" -ForegroundColor Yellow
Write-Host "  Email:    admin@roombuddy.com" -ForegroundColor White
Write-Host "  Password: Admin123!" -ForegroundColor White
Write-Host "  Phone:    +237600000000" -ForegroundColor White
Write-Host ""

Write-Host "Sending request to backend..." -ForegroundColor Yellow

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" `
                                  -Method Post `
                                  -ContentType "application/json" `
                                  -Body $adminData `
                                  -ErrorAction Stop

    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "  SUCCESS! Admin Account Created!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Login Credentials:" -ForegroundColor Yellow
    Write-Host "  Email:    admin@roombuddy.com" -ForegroundColor White
    Write-Host "  Password: Admin123!" -ForegroundColor White
    Write-Host ""
    Write-Host "Next Steps:" -ForegroundColor Yellow
    Write-Host "  1. Go to your RoomBuddy app" -ForegroundColor White
    Write-Host "  2. Login with the credentials above" -ForegroundColor White
    Write-Host "  3. Go to 'Pending Listings' in sidebar" -ForegroundColor White
    Write-Host "  4. Approve your listings!" -ForegroundColor White
    Write-Host ""
    
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    
    if ($statusCode -eq 409) {
        Write-Host ""
        Write-Host "========================================" -ForegroundColor Yellow
        Write-Host "  Admin Account Already Exists!" -ForegroundColor Yellow
        Write-Host "========================================" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "You can login with:" -ForegroundColor Yellow
        Write-Host "  Email:    admin@roombuddy.com" -ForegroundColor White
        Write-Host "  Password: Admin123!" -ForegroundColor White
        Write-Host ""
    } else {
        Write-Host ""
        Write-Host "========================================" -ForegroundColor Red
        Write-Host "  ERROR!" -ForegroundColor Red
        Write-Host "========================================" -ForegroundColor Red
        Write-Host ""
        Write-Host "Error Details:" -ForegroundColor Yellow
        Write-Host $_.Exception.Message -ForegroundColor Red
        Write-Host ""
        Write-Host "Possible Issues:" -ForegroundColor Yellow
        Write-Host "  1. Backend is not running on http://localhost:8080" -ForegroundColor White
        Write-Host "  2. Backend doesn't allow ADMIN role in registration" -ForegroundColor White
        Write-Host "  3. Network/CORS issue" -ForegroundColor White
        Write-Host ""
        Write-Host "Alternative Solution:" -ForegroundColor Yellow
        Write-Host "  Run this SQL in your database:" -ForegroundColor White
        Write-Host "  UPDATE users SET role = 'ADMIN' WHERE email = 'your@email.com';" -ForegroundColor Cyan
        Write-Host ""
    }
}

Write-Host "Press any key to exit..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
