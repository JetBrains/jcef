echo off
rem Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

call set_env.bat || goto:__exit

set ARTIFACT_DIR=jcef_win_x64

if exist "%JCEF_ROOT_DIR%\%ARTIFACT_DIR%" (
    echo *** delete "%JCEF_ROOT_DIR%\%ARTIFACT_DIR%"...
    rmdir /s /q "%JCEF_ROOT_DIR%\%ARTIFACT_DIR%"
)
if exist "%JCEF_ROOT_DIR%\%ARTIFACT_DIR%.tar.gz" (
    echo *** delete "%JCEF_ROOT_DIR%\%ARTIFACT_DIR%.tar.gz"...
    del /f /q "%JCEF_ROOT_DIR%\%ARTIFACT_DIR%.tar.gz"
)

if "%~1" == "clean" (
    exit /b 0
)

cd "%JCEF_ROOT_DIR%" || goto:__exit

where bash
if %ERRORLEVEL% neq 0 (
    echo *** using c:\cygwin64\bin
    set "PATH=c:\cygwin64\bin;%PATH%"
)

echo *** create modules...
sed -i 's/\r$//' "%JB_TOOLS_DIR%"\common\create_modules.sh || goto:__exit
sed -i 's/\r$//' "%JB_TOOLS_DIR%"\common\common.sh || goto:__exit
bash "%JB_TOOLS_DIR%"\common\create_modules.sh || goto:__exit

echo *** create bundle...
mkdir "%ARTIFACT_DIR%" || goto:__exit
move jmods "%ARTIFACT_DIR%" || goto:__exit
bash -c "tar -cvzf $ARTIFACT_DIR.tar.gz -C $ARTIFACT_DIR $(ls $ARTIFACT_DIR)" || goto:__exit
rmdir /s /q %ARTIFACT_DIR% || goto:__exit
dir %ARTIFACT_DIR%.tar.gz || goto:__exit
copy %OUT_CLS_DIR%\jcef-tests.jar . || goto:__exit

cd "%JB_TOOLS_OS_DIR%"
exit /b 0

echo *** SUCCESSFUL
cd "%JB_TOOLS_OS_DIR%" && exit /b 0

:__exit
cd "%JB_TOOLS_OS_DIR%"
echo *** BUILD FAILED
exit /b 1
