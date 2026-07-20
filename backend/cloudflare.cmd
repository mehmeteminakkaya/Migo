@echo off
setlocal
cd /d "%~dp0"
set "NODE_BIN=%USERPROFILE%\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin"
set "PNPM=%USERPROFILE%\.cache\codex-runtimes\codex-primary-runtime\dependencies\bin\fallback\pnpm.cmd"

if not exist "%PNPM%" (
  echo Cloudflare araci bulunamadi. Node.js LTS kurulumu gerekiyor.
  exit /b 1
)

set "PATH=%NODE_BIN%;%PATH%"
call "%PNPM%" exec wrangler %*
exit /b %ERRORLEVEL%
