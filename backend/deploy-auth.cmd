@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0deploy-auth.ps1"
exit /b %ERRORLEVEL%
