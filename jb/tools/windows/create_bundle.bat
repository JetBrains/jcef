echo off
rem Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

call set_env.bat || exit /b 1

set MODULAR_SDK_DIR=%JCEF_ROOT_DIR%\out\win64\modular-sdk
set ARTIFACT=jcef_win_x64
set OS=windows
set ARCH=amd64

if exist "%JCEF_ROOT_DIR%\%ARTIFACT%" (
    echo *** delete "%JCEF_ROOT_DIR%\%ARTIFACT%"...
    rmdir /s /q "%JCEF_ROOT_DIR%\%ARTIFACT%"
)
if exist "%JCEF_ROOT_DIR%\%ARTIFACT%.tar.gz" (
    echo *** delete "%JCEF_ROOT_DIR%\%ARTIFACT%.tar.gz"...
    del /f /q "%JCEF_ROOT_DIR%\%ARTIFACT%.tar.gz"
)

if "%~1" == "clean" (
    exit /b 0
)

cd "%JCEF_ROOT_DIR%" || goto:__exit

echo *** create archive...
where bash
if %ERRORLEVEL% neq 0 (
    echo *** using c:\cygwin64\bin
    set "PATH=c:\cygwin64\bin;%PATH%"
)

bash "%JB_TOOLS_DIR%"\common\bundle_jogl_gluegen.sh

bash -c "[ -d $ARTIFACT ] || mkdir $ARTIFACT" || goto:__exit
bash -c "cp -R jcef_build/native/Release/* $ARTIFACT/" || goto:__exit
bash -c "cp -R $MODULAR_SDK_DIR $ARTIFACT/" || goto:__exit

bash -c "tar -cvzf $ARTIFACT.tar.gz -C $ARTIFACT $(ls $ARTIFACT)" || goto:__exit
bash -c "rm -rf $ARTIFACT" || goto:__exit
bash -c "ls -lah $ARTIFACT.tar.gz" || goto:__exit

echo *** SUCCESSFUL
:__exit
cd "%JB_TOOLS_WIN_DIR%" || exit /b 1
