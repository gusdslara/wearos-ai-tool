@echo off
setlocal
cd /d "%~dp0"
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "ADB=C:\Users\GUS\AppData\Local\Android\Sdk\platform-tools\adb.exe"
set "WATCH=192.168.15.84:42335"
set "LOG=%~dp0build_log.txt"

echo === BUILD AI WEAROS %date% %time% === > "%LOG%"
call gradlew.bat assembleDebug >> "%LOG%" 2>&1
if errorlevel 1 (
  echo BUILD_FAILED >> "%LOG%"
  goto :end
)
echo BUILD_OK >> "%LOG%"
"%ADB%" disconnect %WATCH% >> "%LOG%" 2>&1
"%ADB%" connect %WATCH% >> "%LOG%" 2>&1
timeout /t 3 /nobreak > nul
"%ADB%" devices >> "%LOG%" 2>&1
"%ADB%" -s %WATCH% install -r app\build\outputs\apk\debug\app-debug.apk >> "%LOG%" 2>&1
if errorlevel 1 (
  echo INSTALL_FAILED >> "%LOG%"
  goto :end
)
echo INSTALL_OK >> "%LOG%"
"%ADB%" -s %WATCH% shell am start -n com.wearports.ai/.MainActivity >> "%LOG%" 2>&1
:end
echo === FIM %time% === >> "%LOG%"
endlocal
