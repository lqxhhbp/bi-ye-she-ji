@echo off
rem Start local MySQL (portable). Skip if already running.
tasklist /FI "IMAGENAME eq mysqld.exe" 2>nul | find /I "mysqld.exe" >nul
if %errorlevel%==0 (
  echo MySQL is already running.
  exit /b 0
)
start "mysql" /b "E:\mysql\bin\mysqld.exe" --defaults-file="E:\mysql\my.ini"
echo MySQL starting on port 3306 ...
