@echo off
setlocal enabledelayedexpansion

:: Chuyển thư mục làm việc hiện tại về thư mục gốc của project (thư mục cha của scripts\)
cd /d "%~dp0.."

:: Tạo sẵn thư mục results nếu chưa có
if not exist "results" mkdir "results"

echo ======================================================================
echo [1/3] Compiling Java source files...
echo ======================================================================

javac tools\MemoryLogger.java
javac datastructures\collections\comparators\*.java
javac datastructures\collections\list\*.java
javac datastructures\collections\map\*.java
javac algorithms\sum\*.java
javac algorithms\ulb\*.java
javac -cp . experiments\MainCompareAllDatasets.java

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] 
    pause
    exit /b %ERRORLEVEL%
)
echo [SUCCESS] Bien dich thanh cong.
echo.

echo ======================================================================
echo [2/3] Running Experiments (Saving directly to results\)...
echo ======================================================================
java -cp . experiments.MainCompareAllDatasets

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] error
    pause
    exit /b %ERRORLEVEL%
)
echo.

echo ======================================================================
echo [3/3] Generating Pivot Tables and results\comparison_report.ipynb...
echo ======================================================================
python export_to_notebook.py

if %ERRORLEVEL% NEQ 0 (
    echo [WARNING] python error
)

echo.
echo ======================================================================
echo [COMPLETED] 
echo file: results\comparison_report.ipynb
echo ======================================================================
pause