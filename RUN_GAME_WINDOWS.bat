@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo Starting PROJECT Enigma...
echo.

call gradlew.bat lwjgl3:run --stacktrace
set "GAME_EXIT_CODE=%ERRORLEVEL%"

if not "%GAME_EXIT_CODE%"=="0" (
    echo.
    echo The game could not start. The complete Gradle error is shown above.
    echo Java runtime and compiler detected on this computer:
    java -version
    javac -version
    echo.
    echo Copy the error beginning at "FAILURE: Build failed with an exception"
    echo if you need help with a machine-specific setup problem.
    pause
)

exit /b %GAME_EXIT_CODE%
