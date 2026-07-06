@echo off
setlocal

cd /d "%~dp0"
call mvnw.cmd -q -DskipTests package
if errorlevel 1 exit /b %errorlevel%

java -jar target\XOE-ArcoRotorController-1.0.0.jar
