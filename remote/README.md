# The Chromium Embedded Framework RPC server

## Develop CefServer on windows

1. Run `<PROJECT_ROOT>\jcef\jb\tools\windows\bring_vcpkg.bat`
2. At the end of execution `bring_vcpkg.bat`. Definitions for cmake will be printed in the terminal. I must look like: 
```
-------------------------------------------------------------------------------------------------------
   vcpkg has been successfully installed. Add following definitions to cmake the generation command:
   -DVCPKG_TARGET_TRIPLET=x64-windows-static-cef
   -DCMAKE_TOOLCHAIN_FILE=C:\develop\jcef\third_party\vcpkg\scripts\buildsystems\vcpkg.cmake
-------------------------------------------------------------------------------------------------------
```
3. Run cmake with the above arguments


