@echo off
setlocal enabledelayedexpansion

echo [1/2] Compiling Java source files...
:: Compile core source code
javac tools\MemoryLogger.java
javac algorithms\*.java

:: Compile experimental execution file (Updated to MainTestEx1)
javac -cp . experiments\MainTestEx1.java

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Compilation failed!
    pause
    exit /b %ERRORLEVEL%
)
echo [SUCCESS] Compilation completed.
echo.
echo [2/2] Running experiments.MainTestEx1...
echo ---------------------------------------------------

:: Check if parameters are passed
if not "%1"=="" (
    :: Tu dong them thu muc datasets vao truoc ten file
    set INPUT=datasets/%1
    set OUTPUT=%2
    set MIN_UTILITY=%3
    set REINDUCTION_COUNT=%4
) else (
    echo [INFO] You are running the script without command-line parameters.
    echo Please enter the following parameters or press Enter to use default values.
    echo.

    set /p USER_INPUT="1. Enter Input file name (Enter -> default: DB_Utility.txt): "
    if not "!USER_INPUT!"=="" (
        set INPUT=datasets/!USER_INPUT!
    ) else (
        set INPUT=datasets/DB_Utility.txt
    )

    set OUTPUT=output.txt
    set /p USER_OUTPUT="2. Enter Output file name (Enter -> default: output.txt): "
    if not "!USER_OUTPUT!"=="" set OUTPUT=!USER_OUTPUT!

    set MIN_UTILITY=30
    set /p USER_MIN_UTILITY="3. Enter Min Utility threshold (Enter -> default: 30): "
    if not "!USER_MIN_UTILITY!"=="" set MIN_UTILITY=!USER_MIN_UTILITY!

    set REINDUCTION_COUNT=10
    set /p USER_REINDUCTION_COUNT="4. Enter Max Reinduction Count (Enter -> default: 10): "
    if not "!USER_REINDUCTION_COUNT!"=="" set REINDUCTION_COUNT=!USER_REINDUCTION_COUNT!
    
    echo.
)

:: Run Java with the collected parameters (Updated to MainTestEx1)
java -cp . experiments.MainTestEx1 !INPUT! !OUTPUT! !MIN_UTILITY! !REINDUCTION_COUNT!

echo.
echo ---------------------------------------------------
pause