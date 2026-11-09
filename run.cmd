@echo off
setlocal
pushd "%~dp0"
call mvn -q compile
if errorlevel 1 goto failed
call mvn -q exec:exec
set "result=%errorlevel%"
popd
exit /b %result%
:failed
popd
exit /b 1
