@echo off
setlocal
pushd "%~dp0"
javac -encoding UTF-8 -d out src\*.java
if errorlevel 1 (
    popd
    pause
    exit /b 1
)
if not exist out\assets mkdir out\assets
copy /Y assets\*.png out\assets\ >nul
if errorlevel 1 (
    echo Could not copy game artwork.
    popd
    pause
    exit /b 1
)
java -cp out Main
popd
