@echo off
setlocal

cd /d "%~dp0"

set APP_NAME=Arco Rotor Controller
set APP_VERSION=1.0.2
set MAIN_JAR=XOE-ArcoRotorController-%APP_VERSION%.jar
set MAIN_CLASS=cz.ok1xoe.arcorotor.desktop.ArcoRotorDesktopApplication
set DEST_DIR=target\installer
set TYPE=%~1
if "%TYPE%"=="" set TYPE=msi

call mvnw.cmd -q -DskipTests package
if errorlevel 1 exit /b %errorlevel%

if exist "%DEST_DIR%" rmdir /s /q "%DEST_DIR%"
if not exist "%DEST_DIR%" mkdir "%DEST_DIR%"

jpackage ^
  --name "%APP_NAME%" ^
  --type "%TYPE%" ^
  --dest "%DEST_DIR%" ^
  --input target ^
  --main-jar "%MAIN_JAR%" ^
  --main-class "%MAIN_CLASS%" ^
  --app-version "%APP_VERSION%" ^
  --icon src/main/resources/icons/windows/xoe-arc-compass.ico
if errorlevel 1 exit /b %errorlevel%

echo Installer created in: %DEST_DIR%
