@echo off
setlocal DisableDelayedExpansion
pushd "%~dp0"
if errorlevel 1 exit /b 1
set "SUM_JAVAC="
if defined SUM_JDK goto explicit_jdk
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" set "SUM_JAVAC=%JAVA_HOME%\bin\javac.exe"
if defined SUM_JAVAC goto have_jdk
for /f "delims=" %%P in ('where javac.exe 2^>nul') do if not defined SUM_JAVAC set "SUM_JAVAC=%%P"
if defined SUM_JAVAC goto have_jdk
for /d %%E in ("%USERPROFILE%\.vscode\extensions\redhat.java-*") do for /d %%J in ("%%~fE\jre\*") do if exist "%%~fJ\bin\javac.exe" set "SUM_JAVAC=%%~fJ\bin\javac.exe"
if defined SUM_JAVAC goto have_jdk
echo ERROR: JDK 25 not found. Set JAVA_HOME or SUM_JDK to a JDK 25 folder.
goto failed
:explicit_jdk
set "SUM_JAVAC=%SUM_JDK%\bin\javac.exe"
if not exist "%SUM_JAVAC%" goto bad_jdk
:have_jdk
for %%P in ("%SUM_JAVAC%") do set "SUM_JAVA=%%~dpPjava.exe"
if not exist "%SUM_JAVA%" goto bad_jdk
if not exist "build\classes" mkdir "build\classes"
if errorlevel 1 goto failed
echo Compiler: %SUM_JAVAC%
"%SUM_JAVAC%" --release 25 -encoding UTF-8 -d build\classes @sources.txt
if errorlevel 1 goto failed
"%SUM_JAVA%" -Xmx512m -cp build\classes experiments.StudyCLI %*
set "SUM_EXIT=%ERRORLEVEL%"
popd
exit /b %SUM_EXIT%
:bad_jdk
echo ERROR: Invalid JDK. Check SUM_JDK or JAVA_HOME.
:failed
popd
exit /b 1
