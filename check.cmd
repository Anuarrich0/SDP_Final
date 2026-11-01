@echo off
setlocal
pushd "%~dp0"
javac -encoding UTF-8 -Xlint:all -Werror -d out src\*.java tests\*.java
if errorlevel 1 (
    popd
    exit /b 1
)
if not exist out\assets mkdir out\assets
copy /Y assets\*.png out\assets\ >nul
if errorlevel 1 (
    popd
    exit /b 1
)
java -ea -Djava.awt.headless=true -cp out GameChecks
set "check_result=%errorlevel%"
if not "%check_result%"=="0" goto done
if /I "%~1"=="--ui" (
    java -ea -cp out UiChecks
    if errorlevel 1 goto failed
)
:done
popd
exit /b %check_result%
:failed
popd
exit /b 1
