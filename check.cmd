@echo off
setlocal
pushd "%~dp0"
javac -encoding UTF-8 -Xlint:all -Werror -d out src\*.java tests\*.java
if errorlevel 1 (
    popd
    exit /b 1
)
java -ea -Djava.awt.headless=true -cp out GameChecks
set "check_result=%errorlevel%"
popd
exit /b %check_result%
