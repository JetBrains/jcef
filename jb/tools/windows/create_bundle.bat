echo off
rem Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

call set_env.bat || exit /b 1

if "%TARGET_ARCH%" == "arm64" (
    set ARTIFACT=jcef_win_aarch64
else (
    set ARTIFACT=jcef_win_x64
)

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

where bash
if %ERRORLEVEL% neq 0 (
    echo *** using c:\cygwin64\bin
    set "PATH=c:\cygwin64\bin;%PATH%"
)

if "%TARGET_ARCH%" == "arm64" (
    echo *** WARN: jogl and gluegen won't be bundled due to they do not have Windows ARM64 builds.
) else (
    echo *** bundle jogl and gluegen...
    sed -i 's/\r$//' "%JB_TOOLS_DIR%"\common\bundle_jogl_gluegen.sh
    bash "%JB_TOOLS_DIR%"\common\bundle_jogl_gluegen.sh || goto:__exit
)

echo *** copy binaries...
bash -c "[ -d $ARTIFACT ] || mkdir $ARTIFACT" || goto:__exit
bash -c "cp -R jcef_build/native/Release/* $ARTIFACT/" || goto:__exit
bash -c "cp -R $MODULAR_SDK_DIR $ARTIFACT/" || goto:__exit

echo *** create archive...
bash -c "tar -cvzf $ARTIFACT.tar.gz -C $ARTIFACT $(ls $ARTIFACT)" || goto:__exit
bash -c "rm -rf $ARTIFACT" || goto:__exit
bash -c "ls -lah $ARTIFACT.tar.gz" || goto:__exit

echo *** SUCCESSFUL
cd "%JB_TOOLS_OS_DIR%" && exit /b 0

:__exit
cd "%JB_TOOLS_OS_DIR%" && exit /b 1
