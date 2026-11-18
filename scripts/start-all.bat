@echo off
rem Start MySQL (if needed), backend and frontend for local demo.
cd /d "%~dp0"
call start-mysql.bat
start "campus-express backend" cmd /k "%~dp0run-backend.bat"
timeout /t 3 /nobreak >nul
start "campus-express frontend" cmd /k "%~dp0run-frontend.bat"
echo.
echo Backend : http://localhost:8080/api/health
echo Swagger : http://localhost:8080/swagger-ui.html
echo Frontend: http://localhost:5173
