@echo off
rem Stop local MySQL
"E:\mysql\bin\mysqladmin.exe" -u root -p123456 shutdown
echo MySQL stopped.
