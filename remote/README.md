# The Chromium Embedded Framework RPC server

## Developing CefServer on Windows

1. Run `<PROJECT_ROOT>\jcef\jb\tools\windows\bring_vcpkg.bat`(It may not work with cygwin, it's better to use the stock terminal).
2. At the end of execution `bring_vcpkg.bat`. It may take few minutes. Definitions for cmake will be printed in the terminal. I must look like: 
```
-------------------------------------------------------------------------------------------------------
   vcpkg has been successfully installed. Add following definitions to cmake the generation command:
   -DVCPKG_TARGET_TRIPLET=x64-windows-static-cef
   -DCMAKE_TOOLCHAIN_FILE=C:\develop\jcef\third_party\vcpkg\scripts\buildsystems\vcpkg.cmake
-------------------------------------------------------------------------------------------------------
```
3. Run cmake with the above arguments along with `-G "Visual Studio 16 2019"`

## Developing CefServer on macOS
### 1. Get `bison`
```
brew install bison
```
Bison is only needed at the build dependencies in vcpkg stage.
Unfortunately for macOS and Linux bison must be installed separately.

Make sure that the version of bison is `2.7+`.
```
$ PATH="/opt/homebrew/Cellar/bison/3.8.2/bin:$PATH" bison --version
bison (GNU Bison) 3.8.2
Written by Robert Corbett and Richard Stallman.

Copyright (C) 2021 Free Software Foundation, Inc.
This is free software; see the source for copying conditions.  There is NO
warranty; not even for MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
``` 
Probably you already have `bison` in the `PATH` then no need in modifying `PATH`. 
Alternatively you can run:
```
brew link bison --force
```

### 2. Checkout `vcpkg` at any directory:
TODO: automate deploying vcpkg. And build in into the build pipeline
```
git clone https://github.com/Microsoft/vcpkg.git
```
Install dependencies:
```
cd vcpkg
./bootstrap-vcpkg.sh
PATH="/opt/homebrew/Cellar/bison/3.8.2/bin:$PATH" ./vcpkg install thrift boost-filesystem boost-interprocess
```

### 3. Pass this definition to cmake
`-DCMAKE_TOOLCHAIN_FILE=-DCMAKE_TOOLCHAIN_FILE=<vcpkg_checkout>/scripts/buildsystems/vcpkg.cmake`


