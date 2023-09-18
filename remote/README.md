# The Chromium Embedded Framework RPC server

## TL;DR
Just run cmake with `BUILD_CEF_SERVER=1`. Install missing tools if needed. There might be problems with `bison`.
Ensure that it's `2.7+`.

## Build process
In order to build `cef_server`, `BUILD_CEF_SERVER` environment variable has to be set to `1` at the cmake generate 
project step. Most of the dependencies needed to build `cef_server` will be downloaded and built during this step.

Getting in more details:
1. Will be fetched(as git submodule) and bootstrapped(download the binary)
   [vcpkg](https://github.com/microsoft/vcpkg) into [third_party/vcpkg](../third_party/vcpkg).
2. The dependencies(e.g. thrift library and boost) will be downloaded and built for the target platform.
   Directory [vcpkg_triplets](../vcpkg_triplets) contains triplets(some sort of build configuration) for platforms that
   requires changing the standard vcpkg triplets. For other platforms the standard triplets will be used.
3. Thrift compiler will be built for the host platform as well(with the default triplets). It will be copied to 
   [third_party/thrift](../third_party/thrift) directory.
4. After calling the functions `bring_vcpkg()` variable `CMAKE_TOOLCHAIN_FILE` will be set that allows cmake to locate
   the dependencies. See [vcpkg.cmake](../cmake/vcpkg.cmake).
5. Thrift completer will be run to generate RPC interfaces to be implemented.

## Windows build specifics
Just build `cef_server` target. Everything must work out of the box.

## Developing cef_server on macOS
Get `bison` and `pkg-config`.
```
brew install bison pkg-config
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

## Developing cef_server on Ubuntu
Get `bison`, `flex` and `pkg-config`.
```
apt-get install bison flex pkg-config 
```
Make sure that the version of bison is `2.7+`.

On `arm64` hosts `ninja` must be installed to the system:
```
apt-get install ninja-build
```

### Run Java test ap on Linux
1. Build `cef_server` and `shared_mem_helper` cmake targets.
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

## Update vcpkg
1. Run `git pull` in [vcpkg](../third_party/vcpkg) directory.
2. Commit the changes
