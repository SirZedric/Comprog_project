@echo off
setlocal
cd /d "%~dp0"
if not defined ANDROID_HOME if exist "%LOCALAPPDATA%\Android\Sdk" set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
call "%~dp0gradlew.bat" --no-daemon :app:assembleDebug
if errorlevel 1 (
    echo.
    echo Build failed. See the error above. The GitHub Actions build also works without a local Android SDK.
    pause
    exit /b 1
)
echo.
echo APK created: %~dp0app\build\outputs\apk\debug\app-debug.apk
pause
