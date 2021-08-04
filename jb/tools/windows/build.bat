echo off
rem Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

if "%~1" == "" (
    call :help
    exit /b 0
)
if "%~1" == "help" (
    call :help
    exit /b 0
)

if "%~1" == "clean" (
    set CLEAN="clean"
    set "TARGET_ARCH="
) else (
    if not "%~1" == "all" (
        echo error: wrong option, use 'help'
        exit /b 1
    )
    set CLEAN=

    set "TARGET_ARCH=%~2"
    if "%TARGET_ARCH%" == "" (
        set "TARGET_ARCH=x86_64"
    )
)

if not exist ..\..\..\jb\tools\windows (
    echo error: not in 'jb\tools\windows' dir
    goto:__exit
)

echo *** && echo *** BUILD NATIVE && echo ***
call build_native.bat %CLEAN% || goto:__exit

echo *** && echo *** BUILD JAVA && echo ***
call build_java.bat %CLEAN% || goto:__exit

echo *** && echo *** CREATE BUNDLE && echo ***
call create_bundle.bat %CLEAN% || goto:__exit

echo *** BUILD SUCCESSFUL
cd "%JB_TOOLS_OS_DIR%"
exit /b 0

:help
echo "build.bat [option]"
echo "Options:"
echo "  help             - Print this help."
echo "  all [arch]       - Build all the artifacts. Optional 'arch' defines the target CPU: x86_64 (default) or arm64."
echo "  clean            - Clean all the artifacts."
echo "Environment variables:"
echo "  JAVA_HOME        - Path to java home with the same CPU architecture you are building JCEF against (x86_64 or arm64)."
echo "  ANT_HOME         - Path to 'ant' home, or if not set then 'ant' must be in PATH."
echo "  CMAKE_37_PATH    - Path to cmake 3.7 home."
echo "  PYTHON_27_PATH   - Path to python 2.7 exe."
echo "  VS140COMNTOOLS   - Provided with <VS2012 x64 Cross Tools Command Prompt>."
echo "  [VS160COMNTOOLS] - Path to <Visual Studio 2019 installation>\Common7\Tools."
echo "                     Required if you are building for ARM64 ('build.bat all arm64')."
exit /b 0

:__exit
cd "%JB_TOOLS_OS_DIR%"
echo *** BUILD FAILED
exit /b 1
