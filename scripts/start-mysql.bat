@echo off
rem Start local MySQL (portable, hidden window). Skip if already running.
tasklist /FI "IMAGENAME eq mysqld.exe" 2>nul | find /I "mysqld.exe" >nul
if %errorlevel%==0 (
  echo MySQL is already running.
  exit /b 0
)
powershell -NoProfile -Command "Start-Process -FilePath 'E:\mysql\bin\mysqld.exe' -ArgumentList '--defaults-file=E:\mysql\my.ini' -WindowStyle Hidden"
echo MySQL starting on port 3306 ...
