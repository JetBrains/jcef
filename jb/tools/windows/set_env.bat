rem Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

echo off

if not exist set_env.bat (
    echo error: not in jb tools windows dir
    exit /b 1
)

cd ..\..\..

set JCEF_ROOT_DIR=%cd%
set JB_TOOLS_DIR=%JCEF_ROOT_DIR%\jb\tools
set JB_TOOLS_OS_DIR=%JB_TOOLS_DIR%\windows
set MODULAR_SDK_DIR=%JCEF_ROOT_DIR%\out\win64\modular-sdk
set OS=windows
if "%TARGET_ARCH%" == "arm64" (
    set DEPS_ARCH=arm64
) else (
    set DEPS_ARCH=amd64
)

cd "%JB_TOOLS_OS_DIR%" || exit /b 1

if "%env.ANT_HOME%" neq "" (
    set ANT_HOME=%env.ANT_HOME%
)
where ant
if %ERRORLEVEL% neq 0 (
    if "%ANT_HOME%" == "" (
        echo error: env.ANT_HOME is not set
        exit /b 1
    )
    echo ANT_HOME=%ANT_HOME%
    set "PATH=%ANT_HOME%\bin;%PATH%"
)

if "%env.JDK_11%" neq "" (
    set JDK_11=%env.JDK_11%
)
if "%JDK_11%" == "" (
    echo error: JDK_11 is not set
    exit /b 1
)
set JAVA_HOME=%JDK_11%
echo JAVA_HOME=%JAVA_HOME%
