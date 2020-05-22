echo off
rem Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

call set_env.bat || exit /b 1

set OUT_DIR=%JCEF_ROOT_DIR%\jcef_build\native\Release

if "%~1" == "clear" (
    echo *** delete "%OUT_DIR%"...
    rmdir /s /q "%OUT_DIR%"
    exit /b 0
)
md "%OUT_DIR%"

cd "%JCEF_ROOT_DIR%\jcef_build" || exit /b 1

echo *** set VS14 env...
if "%env.VS140COMNTOOLS%" neq "" (
    set VS140COMNTOOLS=%env.VS140COMNTOOLS%
)
if "%VS140COMNTOOLS%" == "" (
    echo error: VS140COMNTOOLS is not set
    exit /b 1
)
echo VS140COMNTOOLS=%VS140COMNTOOLS%
call "%VS140COMNTOOLS%\..\..\VC\vcvarsall.bat" amd64 || exit /b 1

echo *** run cmake...
if "%env.CMAKE_37_PATH%" neq "" (
    set CMAKE_37_PATH=%env.CMAKE_37_PATH%
)
if "%CMAKE_37_PATH%" == "" (
    echo error: CMAKE_37_PATH is not set
    exit /b 1
)
echo CMAKE_37_PATH=%CMAKE_37_PATH%

if "%env.PYTHON_27_PATH%" neq "" (
    set PYTHON_27_PATH=%env.PYTHON_27_PATH%
)
if "%PYTHON_27_PATH%" == "" (
    echo error: PYTHON_27_PATH is not set
    exit /b 1
)
echo PYTHON_27_PATH=%PYTHON_27_PATH%
set "PATH=%CMAKE_37_PATH%\bin;%PYTHON_27_PATH%;%PATH%"
set RC=
cmake -G "Visual Studio 14 Win64" .. || exit /b 1

echo *** run MSBuild.exe...
"c:\Program Files (x86)\MSBuild\14.0\Bin\MSBuild.exe" /t:Rebuild /p:Configuration=Release .\jcef.sln || exit /b 1

cd "%JB_TOOLS_WIN_DIR%" || exit /b 1