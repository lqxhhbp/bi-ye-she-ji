@echo off
rem Run Spring Boot backend (Ctrl+C to stop).
cd /d "%~dp0..\campus-express"
call mvn -DskipTests spring-boot:run
