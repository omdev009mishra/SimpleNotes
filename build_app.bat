@echo off
echo Compiling...
if not exist bin mkdir bin
javac -d bin -sourcepath SimpleNotes/src SimpleNotes/src/*.java
if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b %errorlevel%
)

echo Creating JAR...
jar cfm SimpleNotes.jar MANIFEST.MF -C bin .
if %errorlevel% neq 0 (
    echo JAR creation failed!
    pause
    exit /b %errorlevel%
)

echo.
echo Build Successful!
echo Created SimpleNotes.jar
echo.
echo You can now run the app by double-clicking SimpleNotes.jar
echo or by running the 'run_app.bat' script.
pause
