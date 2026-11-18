@echo off
setlocal
cd /d "%~dp0.."
echo ===== Java =====
java -version 2>&1
echo.
echo ===== Maven =====
call mvn -v 2>&1 | findstr /B /C:"Apache Maven"
echo.
echo ===== Node / npm =====
node -v
call npm -v
echo.
echo ===== MySQL =====
mysql --version
mysqladmin -u root -p123456 ping 2>&1
echo.
echo ===== MySQL databases =====
mysql -u root -p123456 -N -e "show databases;" 2>nul
echo.
echo ===== Frontend dependencies =====
if exist "campus-express-web\node_modules" (echo node_modules: OK) else (echo node_modules: MISSING)
echo.
echo ===== Backend dependencies =====
if exist "campus-express\target" (echo backend target: OK) else (echo backend target: not built yet)
echo.
echo ===== Ports =====
netstat -ano | findstr /R /C:":8080 .*LISTENING" /C:":5173 .*LISTENING" /C:":3306 .*LISTENING"
