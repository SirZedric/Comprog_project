@echo off
setlocal
set "QUIZ_ROOT=%~dp0"

if not defined JAVA_HOME if exist "%ProgramFiles%\Android\Android Studio\jbr\bin\java.exe" set "JAVA_HOME=%ProgramFiles%\Android\Android Studio\jbr"
if not defined JAVA_HOME if exist "%LOCALAPPDATA%\Programs\Android Studio\jbr\bin\java.exe" set "JAVA_HOME=%LOCALAPPDATA%\Programs\Android Studio\jbr"

powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%QUIZ_ROOT%scripts\prepare-wrapper.ps1"
if errorlevel 1 exit /b 1

set "QUIZ_JAVA=java.exe"
if defined JAVA_HOME set "QUIZ_JAVA=%JAVA_HOME%\bin\java.exe"
"%QUIZ_JAVA%" -Dorg.gradle.appname=gradlew -classpath "%QUIZ_ROOT%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
exit /b %ERRORLEVEL%
