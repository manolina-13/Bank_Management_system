@echo off
echo Shutting down Tomcat...
call "C:\Program Files\Apache Software Foundation\Tomcat 9.0\bin\shutdown.bat"

REM Optional delay (wait 5 seconds)
timeout /t 5 /nobreak >nul

echo Starting up Tomcat...
call "C:\Program Files\Apache Software Foundation\Tomcat 9.0\bin\startup.bat"
