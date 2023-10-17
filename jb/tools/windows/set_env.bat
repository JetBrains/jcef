rem Copyright 2000-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

echo off

if not exist set_env.bat (
    echo error: not in jb tools windows dir
    exit /b 1
)

cd ..\..\..

set JCEF_ROOT_DIR=%cd%
set JB_TOOLS_DIR=%JCEF_ROOT_DIR%\jb\tools
set JB_TOOLS_OS_DIR=%JB_TOOLS_DIR%\windows
set OUT_CLS_DIR=%JCEF_ROOT_DIR%\out\win64
set OUT_NATIVE_DIR=%JCEF_ROOT_DIR%\jcef_build\native\Release
set OUT_REMOTE_DIR=%JCEF_ROOT_DIR%\jcef_build\remote\Release

set OS=windows
set ORIGINAL_PATH=%PATH%
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

if "%JAVA_HOME%" == "" (
    echo error: JAVA_HOME is not set
    exit /b 1
)
if "%JAVA_HOME%" == "" (
    set "JAVA_HOME=%JAVA_HOME:\=/%"
)
echo JAVA_HOME=%JAVA_HOME%

exit /b 0