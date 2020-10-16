echo off
rem Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

call set_env.bat || goto:__exit

set OUT_DIR=%JCEF_ROOT_DIR%\out

if "%~1" == "clean" (
    echo *** delete "%OUT_DIR%"...
    rmdir /s /q "%OUT_DIR%"
    exit /b 0
)
md "%OUT_DIR%"

echo *** compile java...
cd "%JCEF_ROOT_DIR%\tools" || goto:__exit
echo cd=%cd%
call compile.bat win64 Release || goto:__exit

cd "%JB_TOOLS_OS_DIR%"
exit /b 0

:__exit
cd "%JB_TOOLS_OS_DIR%"
echo *** BUILD FAILED
exit /b 1
