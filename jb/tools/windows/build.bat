echo off
rem Copyright 2000-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

call set_env.bat || exit /b 1

call set_env.bat || exit /b 1

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

if "%~2" == "" (
    set "TARGET_ARCH=x86_64"
) else (
    set "TARGET_ARCH=%~2"
)

if not exist ..\..\..\jb\tools\windows (
    echo error: not in 'jb\tools\windows' dir
    goto:__exit
)

if "%TEAMCITY_VERSION%" neq "" (
    set JCEF_CLEAN_VCPKG=1
)

if "%JCEF_CLEAN_VCPKG%" neq "" (
    echo "Cleaning up vcpkg..."
    pushd .
    cd ../../..
    git submodule foreach --recursive git clean -xfdf
    git submodule foreach --recursive git reset --hard
    git submodule update --init --recursive
    popd
)

echo *** && echo *** BUILD NATIVE && echo ***
call build_native.bat %CLEAN% || goto:__exit

echo *** && echo *** BUILD JAVA && echo ***
call build_java.bat %CLEAN% || goto:__exit

echo *** && echo *** CREATE BUNDLE && echo ***
call create_bundle.bat %CLEAN% || goto:__exit

echo *** BUILD SUCCESSFUL
set PATH=%ORIGINAL_PATH% && cd "%JB_TOOLS_OS_DIR%" && exit /b 0

:help
echo "build.bat [option]"
echo "Options:"
echo "  help               - Print this help."
echo "  all [arch]         - Build all the artifacts. Optional 'arch' defines the target CPU: x86_64 (default) or arm64."
echo "  clean [arch]       - Clean all the artifacts. Optional 'arch' is the same as for the 'all' target (x86_64 is default)."
echo "Environment variables:"
echo "  JAVA_HOME          - Path to java home."
echo "  ANT_HOME           - Path to 'ant' home, or if not set then 'ant' must be in PATH."
echo "  JCEF_CMAKE         - Path to CMake home, version 3.14.7 or above."
echo "  [JCEF_JNI]         - Optional. Path to java home with the same CPU architecture you are building JCEF against (x86_64 or arm64)."
echo "                       Set it if you want to cross build JCEF (x86_64 -> arm64)."
echo "                       Equals to JAVA_HOME by default."
echo "  PYTHON_27_PATH     - Path to python 2.7 exe."
echo "  VS160COMNTOOLS     - Path to <Visual Studio 2019 installation>\Common7\Tools."
echo "  JCEF_CLEANUP_VCPKG - Cleanup the checkout dir before build"
set PATH=%ORIGINAL_PATH% && cd "%JB_TOOLS_OS_DIR%" && exit /b 0

:__exit
echo *** BUILD FAILED
set PATH=%ORIGINAL_PATH% && cd "%JB_TOOLS_OS_DIR%" && exit /b 1
