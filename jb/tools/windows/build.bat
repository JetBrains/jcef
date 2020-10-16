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
) else (
    if not "%~1" == "all" (
        echo error: wrong option, use 'help'
        exit /b 1
    )
    set CLEAN=
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
echo "  help            - Print this help."
echo "  all             - Build all the artifacts."
echo "  clean           - Clean all the artifacts."
echo "Environment variables:"
echo "  JDK_11          - Path to OpenJDK 11 home."
echo "  ANT_HOME        - Path to 'ant' home, or if not set then 'ant' must be in PATH."
echo "  CMAKE_37_PATH   - Path to cmake 3.7 home."
echo "  PYTHON_27_PATH  - Path to python 2.7 exe."
echo "  VS140COMNTOOLS  - Provided with <VS2012 x64 Cross Tools Command Prompt>."
exit /b 0

:__exit
cd "%JB_TOOLS_OS_DIR%"
echo *** BUILD FAILED
exit /b 1
