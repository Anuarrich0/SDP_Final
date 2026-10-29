@echo off
setlocal
pushd "%~dp0"
javac -encoding UTF-8 -d out src\*.java
if errorlevel 1 (
    popd
    pause
    exit /b 1
)
java -cp out Main
popd
