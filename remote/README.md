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

## Developing CefServer on Ubuntu
### 1. Get `bison`
```
apt-get install bison
```
Bison is only needed at the build dependencies in vcpkg stage.
Unfortunately for macOS and Linux bison must be installed separately.

Make sure that the version of bison is `2.7+`.
```
$ bison --version
bison (GNU Bison) 3.8.2
Written by Robert Corbett and Richard Stallman.

Copyright (C) 2021 Free Software Foundation, Inc.
This is free software; see the source for copying conditions.  There is NO
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
./vcpkg install thrift boost-filesystem boost-interprocess
```

### 3. Pass following definition to cmake
```
-DCMAKE_TOOLCHAIN_FILE=<vcpkg_checkout>/scripts/buildsystems/vcpkg.cmake
-DPROJECT_ARCH=x86_64
```
Replace `x86_64` with `arm64` for ARM hardware.

Don't forget to set `JAVA_HOME`.

### 4. Run Java test ap on Linux
1. Build `CefServer` and `shared_mem_helper` cmake targets.
2. Open gradle project at `<project_root>/jb/project/java-gradle/build.gradle` in IDEA.
3. Navigate to `<project_root>/java_tests/tests/remote/TestApp.java`.
4. Run `TestApp.main()`. It will fail.
5. Go to the Run configurations in IDEA and specify the path to `shared_mem_helper` in the environment variables.
   It must be something like:
```
LD_LIBRARY_PATH=/home/khvv/develop/jcef/cmake-build-release/remote/Debug
```

## Thrift files code format
Assuming `python` and `pip` are installed.
```
pip install thrift-fmt
cd <project_root>
thrift-fmt -w -r remote
```
