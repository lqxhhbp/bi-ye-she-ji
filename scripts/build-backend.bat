@echo off
rem Build backend jar (skip tests).
cd /d "%~dp0..\campus-express"
call mvn clean package -DskipTests
echo.
echo Jar: %~dp0..\campus-express\target\campus-express-1.0.0.jar
