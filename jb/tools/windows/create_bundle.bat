echo off
rem Copyright 2000-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

call set_env.bat || goto:__exit

if "%TARGET_ARCH%" == "arm64" (
    set ARTIFACT_DIR=jcef_win_aarch64
    set ARTIFACT_SERVER=cef_server_win_aarch64
) else (
    set ARTIFACT_DIR=jcef_win_x64
    set ARTIFACT_SERVER=cef_server_win_x64
)

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
bash "%JB_TOOLS_DIR%"\common\create_modules.sh || goto:__exit

echo *** create bundle...
mkdir "%ARTIFACT_DIR%" || goto:__exit
move jmods "%ARTIFACT_DIR%" || goto:__exit

echo *** create cef_server bundle...
if exist "%JCEF_ROOT_DIR%\%ARTIFACT_SERVER%.tar.gz" (
    echo *** delete "%JCEF_ROOT_DIR%\%ARTIFACT_SERVER%.tar.gz"...
    del /f /q "%JCEF_ROOT_DIR%\%ARTIFACT_SERVER%.tar.gz"
)

bash -c "tar -cvzf $ARTIFACT_SERVER.tar.gz -C cef_server $(ls cef_server)" || goto:__exit
rmdir /s /q cef_server || goto:__exit

echo *** create native standalone bundle...
if exist native_bundle rd /s /q native_bundle
mkdir native_bundle
xcopy /E /Y "%OUT_REMOTE_DIR%\bin\*" native_bundle\
xcopy /E /Y "%OUT_REMOTE_DIR%\lib\*" native_bundle\
copy /Y "%OUT_REMOTE_DIR%\shared_mem_helper.dll" native_bundle\
copy /Y "%OUT_NATIVE_DIR%\jcef.dll" native_bundle\
copy /Y "%OUT_NATIVE_DIR%\jcef_helper.dll" native_bundle\
copy /Y "%OUT_NATIVE_DIR%\jcef_helper.exe" native_bundle\
del /f /q native_bundle\*.pdb
del /f /q native_bundle\*.log

sed -i "s/\r$//" "%JB_TOOLS_DIR%"\common\create_version_file.sh
bash "%JB_TOOLS_DIR%"\common\create_version_file.sh %ARTIFACT_DIR%

bash -c "tar -cvzf $ARTIFACT_DIR.tar.gz -C $ARTIFACT_DIR $(ls $ARTIFACT_DIR)" || goto:__exit
rmdir /s /q %ARTIFACT_DIR% || goto:__exit
dir %ARTIFACT_DIR%.tar.gz || goto:__exit
copy %OUT_CLS_DIR%\jcef-tests.jar . || goto:__exit

cd "%JB_TOOLS_OS_DIR%"
exit /b 0

echo *** SUCCESSFUL
set PATH=%ORIGINAL_PATH% && cd "%JB_TOOLS_OS_DIR%" && exit /b 0

:__exit
echo *** BUILD FAILED
set PATH=%ORIGINAL_PATH% && cd "%JB_TOOLS_OS_DIR%" && exit /b 1
