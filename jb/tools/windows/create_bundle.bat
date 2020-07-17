echo off
rem Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

call set_env.bat || exit /b 1

set ARTIFACT=jcef_win_x64

echo *** delete "%JCEF_ROOT_DIR%\%ARTIFACT%"...
rmdir /s /q "%JCEF_ROOT_DIR%\%ARTIFACT%"
del /f /q "%JCEF_ROOT_DIR%\%ARTIFACT%.tar.gz"

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

set MODULAR_SDK=out/win64/modular-sdk

rem temp exclude jogl
bash -c "cat $MODULAR_SDK/modules_src/jcef/module-info.java | grep -v jogl | grep -v gluegen > __tmp" || goto:__exit
bash -c "mv __tmp $MODULAR_SDK/modules_src/jcef/module-info.java" || goto:__exit

bash -c "[ -d $ARTIFACT ] || mkdir $ARTIFACT" || goto:__exit
bash -c "cp -R jcef_build/native/Release/* $ARTIFACT/" || goto:__exit
bash -c "cp -R $MODULAR_SDK $ARTIFACT/" || goto:__exit

bash -c "tar -cvzf $ARTIFACT.tar.gz -C $ARTIFACT $(ls $ARTIFACT)" || goto:__exit
bash -c "rm -rf $ARTIFACT" || goto:__exit
bash -c "ls -lah $ARTIFACT.tar.gz" || goto:__exit

echo *** SUCCESSFUL
:__exit
cd "%JB_TOOLS_WIN_DIR%" || exit /b 1
