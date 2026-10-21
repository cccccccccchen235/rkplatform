@echo off
rem ===============================================================
rem  Local one-key quality gate (23 section 4.2) -- thin launcher.
rem
rem  This .cmd is intentionally ASCII-only: cmd.exe reads .cmd files
rem  in the console code page (GBK here), so UTF-8 Chinese comments
rem  get mangled into stray commands. All logic + Chinese output live
rem  in run-all.ps1, which is UTF-8 with BOM.
rem
rem  Usage:  run-all.cmd            gates 1-4 (+5 if the backend is up)
rem          run-all.cmd -Gate5     gate 5 only (backend must be up)
rem ===============================================================
rem Console code page -> UTF-8, so bash's UTF-8 output and this gate's Chinese
rem output are consistent (run-all.ps1 also sets [Console]::OutputEncoding).
chcp 65001 >nul 2>&1
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0run-all.ps1" %*
exit /b %ERRORLEVEL%
