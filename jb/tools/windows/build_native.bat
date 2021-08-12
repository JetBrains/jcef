echo off
rem Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

call set_env.bat || exit /b 1

set OUT_DIR=%JCEF_ROOT_DIR%\jcef_build\native\Release

if "%~1" == "clean" (
    echo *** delete "%OUT_DIR%"...
    rmdir /s /q "%OUT_DIR%"
    exit /b 0
)
md "%OUT_DIR%"

if "%~2" == "arm64" (
    set "TARGET_ARCH=arm64"
)
if "%~2" == "x86_64" (
    set "TARGET_ARCH=x86_64"
)
echo TARGET_ARCH=%TARGET_ARCH%

cd "%JCEF_ROOT_DIR%\jcef_build" || goto:__exit

if "%TARGET_ARCH%" == "arm64" (
    echo *** set VS16 env targeting ARM64...
    if "%env.VS160COMNTOOLS%" neq "" (
        set "VS160COMNTOOLS=%env.VS160COMNTOOLS%"
    )
    if "%VS160COMNTOOLS%" == "" (
        echo error: VS160COMNTOOLS is not set
        goto:__exit
    )
    echo VS160COMNTOOLS=%VS160COMNTOOLS%
    call "%VS160COMNTOOLS%\..\..\VC\Auxiliary\Build\vcvarsamd64_arm64.bat" || goto:__exit
) else (
    echo *** set VS14 env...
    if "%env.VS140COMNTOOLS%" neq "" (
        set "VS140COMNTOOLS=%env.VS140COMNTOOLS%"
    )
    if "%VS140COMNTOOLS%" == "" (
        echo error: VS140COMNTOOLS is not set
        goto:__exit
    )
    echo VS140COMNTOOLS=%VS140COMNTOOLS%
    call "%VS140COMNTOOLS%\..\..\VC\vcvarsall.bat" amd64 || goto:__exit
)

echo *** run cmake...
if "%env.JCEF_CMAKE%" neq "" (
    set "JCEF_CMAKE=%env.JCEF_CMAKE%"
)
if "%JCEF_CMAKE%" == "" (
    echo error: JCEF_CMAKE is not set
    goto:__exit
)
echo JCEF_CMAKE=%JCEF_CMAKE%

if "%env.PYTHON_27_PATH%" neq "" (
    set PYTHON_27_PATH=%env.PYTHON_27_PATH%
)
if "%PYTHON_27_PATH%" == "" (
    echo error: PYTHON_27_PATH is not set
    goto:__exit
)
echo PYTHON_27_PATH=%PYTHON_27_PATH%
set "PATH=%JCEF_CMAKE%\bin;%PYTHON_27_PATH%;%PATH%"
set RC=
if "%TARGET_ARCH%" == "arm64" (
    cmake -G "Visual Studio 16 2019" -A ARM64 .. || goto:__exit
) else (
    cmake -G "Visual Studio 14 Win64" .. || goto:__exit
)

echo *** run MSBuild.exe...
"c:\Program Files (x86)\MSBuild\14.0\Bin\MSBuild.exe" /t:Rebuild /p:Configuration=Release .\jcef.sln || goto:__exit

cd "%JB_TOOLS_OS_DIR%" && exit /b 0

:__exit
cd "%JB_TOOLS_OS_DIR%" && exit /b 1