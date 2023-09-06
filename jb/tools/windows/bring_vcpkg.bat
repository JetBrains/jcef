@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
set "OVERLAY_TRIPLET_DIR=%SCRIPT_DIR%\vcpkg_triplets"
set "ROOT_DIR=%SCRIPT_DIR%\..\..\.."
for %%i in ("%ROOT_DIR%") do set VCPKG_PATH=%%~fi\third_party\vcpkg

set CLEAN_VCPKG_DIR=No
for %%a in (%*) do (
    if "%%a"=="/c" set CLEAN_VCPKG_DIR=Yes
)

echo Installing vcpkg configuration:
if "%TARGET_ARCH%"=="arm64" (
  set VCPKG_TRIPLET=arm64-windows-static-cef
) else if "%TARGET_ARCH%"=="x86_64" (
  set VCPKG_TRIPLET=x64-windows-static-cef
) else if "%PROCESSOR_ARCHITECTURE%"=="AMD64" (
  set VCPKG_TRIPLET=x64-windows-static-cef
) else if "%PROCESSOR_ARCHITECTURE%"=="ARM64" (
  set VCPKG_TRIPLET=arm64-windows-static-cef
) else if "%PROCESSOR_ARCHITECTURE%"=="x86" (
  set VCPKG_TRIPLET=x86-windows-static-cef
) else (
  echo %PROCESSOR_ARCHITECTURE% architecture is not supported
  exit /b 1
)

set PACKAGES=boost-filesystem boost-interprocess thrift

echo * VCPKG installation dir:              %VCPKG_PATH%
echo * Processor architecture:              %PROCESSOR_ARCHITECTURE%
echo * Overlay triplets dir:                %OVERLAY_TRIPLET_DIR%
echo * VCPKG triplet:                       %VCPKG_TRIPLET%
echo * Packages to install:                 %PACKAGES%
echo * Cleanup destination directory(/c):   %CLEAN_VCPKG_DIR%

if exist %VCPKG_PATH% if "%CLEAN_VCPKG_DIR%"=="Yes" (
  echo.
  echo Cleaning up vcpkg directory...
  rmdir /s /q %VCPKG_PATH%
)

echo.
if not exist %VCPKG_PATH% (
  echo Fetching vcpkg...
  git clone https://github.com/Microsoft/vcpkg.git %VCPKG_PATH%
) else (
  echo Fetching vcpkg...Skipped(Run with /c to force fetching new version)
)

if not exist %VCPKG_PATH%\vcpkg.exe (
  echo.
  echo Bootstrapping...
  call %VCPKG_PATH%\bootstrap-vcpkg.bat -disableMetrics
) else (
  echo Bootstrapping...Skipped
)

for %%P in (%PACKAGES%) do (
  echo.
  echo Installing %%P:%VCPKG_TRIPLET%...
  %VCPKG_PATH%\vcpkg.exe install %%P:%VCPKG_TRIPLET% --overlay-triplets %OVERLAY_TRIPLET_DIR%
)

endlocal & (
  set VCPKG_PATH=%VCPKG_PATH%
  set VCPKG_TRIPLET=%VCPKG_TRIPLET%
)

echo.
echo.
echo -------------------------------------------------------------------------------------------------------
echo    vcpkg has been successfully installed. Add following definitions to cmake the generation command:
echo    -DVCPKG_TARGET_TRIPLET=%VCPKG_TRIPLET%
echo    -DCMAKE_TOOLCHAIN_FILE=%VCPKG_PATH%\scripts\buildsystems\vcpkg.cmake
echo -------------------------------------------------------------------------------------------------------

