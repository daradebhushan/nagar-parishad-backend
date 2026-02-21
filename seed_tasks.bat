@echo off
set "URL=http://localhost:8080/api/public/whatsapp"
set "FROM=whatsapp:+918888888888"
set "TO=whatsapp:+14155238886"

echo Resetting...
curl -X POST %URL% -d "From=%FROM%&To=%TO%&Body=Hi"
timeout /t 2 /nobreak >nul

echo Language...
curl -X POST %URL% -d "From=%FROM%&To=%TO%&Body=1"
timeout /t 2 /nobreak >nul

echo Dept...
curl -X POST %URL% -d "From=%FROM%&To=%TO%&Body=1"
timeout /t 2 /nobreak >nul

echo Name...
curl -X POST %URL% -d "From=%FROM%&To=%TO%&Body=Task Flow Verify"
timeout /t 2 /nobreak >nul

echo Desc...
curl -X POST %URL% -d "From=%FROM%&To=%TO%&Body=Direct verification for task creation"
timeout /t 2 /nobreak >nul

echo Skip Photo...
curl -X POST %URL% -d "From=%FROM%&To=%TO%&Body=Skip"
timeout /t 2 /nobreak >nul

echo Location...
curl -X POST %URL% -d "From=%FROM%&To=%TO%&Body=Pune"
timeout /t 2 /nobreak >nul

echo Seed Complete.
