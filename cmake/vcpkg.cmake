#set(CMAKE_TOOLCHAIN_FILE "${CMAKE_CURRENT_SOURCE_DIR}/vcpkg/scripts/buildsystems/vcpkg.cmake"
#        CACHE STRING "Vcpkg toolchain file")

function(bring_vcpkg)
    set(JCEF_VCPKG_DIRECTORY ${CMAKE_SOURCE_DIR}/third_party/vcpkg)

    if (NOT DEFINED PROJECT_ARCH)
        message(FATAL_ERROR "PROJECT_ARCH is not defined. It's expected to be arm64 or x86_64")
    endif ()

    if (${PROJECT_ARCH} STREQUAL "arm64")
        set(VCPKG_ARCH arm64)
    elseif (${PROJECT_ARCH} STREQUAL "x86_64")
        set(VCPKG_ARCH x64)
    elseif (${PROJECT_ARCH} STREQUAL "x86")
        set(VCPKG_ARCH x86)
    endif ()

    if ("${CMAKE_HOST_SYSTEM_NAME}" STREQUAL "Windows")
        set(VCPKG_TARGET_TRIPLET "${VCPKG_ARCH}-windows-static-jcef")
    elseif ("${CMAKE_HOST_SYSTEM_NAME}" STREQUAL "Linux")
        set(VCPKG_TARGET_TRIPLET "${VCPKG_ARCH}-linux")
        if ("${VCPKG_ARCH}" STREQUAL "arm64")
            set(ENV{VCPKG_FORCE_SYSTEM_BINARIES} 1)
        endif ()
    elseif ("${CMAKE_HOST_SYSTEM_NAME}" STREQUAL "Darwin")
        set(VCPKG_TARGET_TRIPLET "${VCPKG_ARCH}-osx-jcef")
    else ()
        message(FATAL_ERROR "Unknown OS: ${CMAKE_HOST_SYSTEM_NAME}")
    endif ()

    find_package(Git REQUIRED)
    execute_process(
            COMMAND ${GIT_EXECUTABLE} submodule update --init --recursive
            WORKING_DIRECTORY ${CMAKE_SOURCE_DIR}
            RESULT_VARIABLE RESULT)
    if (RESULT)
        message(FATAL_ERROR "Failed to get vcpkg. Result: ${RESULT}")
    endif ()

    if ("${CMAKE_HOST_SYSTEM_NAME}" STREQUAL "Windows")
        if (NOT EXISTS ${JCEF_VCPKG_DIRECTORY}/vcpkg.exe)
            execute_process(
                    COMMAND cmd /C ${JCEF_VCPKG_DIRECTORY}/bootstrap-vcpkg.bat -disableMetrics
                    WORKING_DIRECTORY ${JCEF_VCPKG_DIRECTORY}
                    RESULT_VARIABLE RESULT)
        endif ()
    else ()
        if (NOT EXISTS ${JCEF_VCPKG_DIRECTORY}/vcpkg)
            execute_process(
                    COMMAND bash ${JCEF_VCPKG_DIRECTORY}/bootstrap-vcpkg.sh -disableMetrics
                    WORKING_DIRECTORY ${JCEF_VCPKG_DIRECTORY}
                    RESULT_VARIABLE RESULT)
        endif ()
    endif ()
    if (RESULT)
        message(FATAL_ERROR "Failed to bootstrap vcpkg. Result: ${RESULT}")
    endif ()

    if (NOT PROJECT_ARCH)
        if (UNIX)
            execute_process(COMMAND uname -m OUTPUT_VARIABLE PROJECT_ARCH)
            string(STRIP ${PROJECT_ARCH} PROJECT_ARCH)
        endif ()
    endif ()

    if (NOT PROJECT_ARCH)
        message(FATAL_ERROR "PROJECT_ARCH expected to be arm64, x86_64 or x86. Actual value: '${PROJECT_ARCH}'")
    endif ()

    set(JCEF_VCPKG_DIRECTORY ${JCEF_VCPKG_DIRECTORY} PARENT_SCOPE)
    set(CMAKE_TOOLCHAIN_FILE "${JCEF_VCPKG_DIRECTORY}/scripts/buildsystems/vcpkg.cmake" PARENT_SCOPE)
    set(VCPKG_TARGET_TRIPLET ${VCPKG_TARGET_TRIPLET} PARENT_SCOPE)
endfunction()

function(vcpkg_install_package)
    foreach (PKG IN LISTS ARGN)
        message("Run: ${JCEF_VCPKG_DIRECTORY}/vcpkg install ${PKG}:${VCPKG_TARGET_TRIPLET}")
        if ("${CMAKE_HOST_SYSTEM_NAME}" STREQUAL "Darwin")
            execute_process(
                    COMMAND ${JCEF_VCPKG_DIRECTORY}/vcpkg install ${PKG}:${VCPKG_TARGET_TRIPLET} --overlay-triplets ${CMAKE_SOURCE_DIR}/vcpkg_triplets/mac
                    WORKING_DIRECTORY ${CMAKE_SOURCE_DIR}
                    RESULT_VARIABLE RESULT)
        elseif ("${CMAKE_HOST_SYSTEM_NAME}" STREQUAL "Windows")
            execute_process(
                    COMMAND ${JCEF_VCPKG_DIRECTORY}/vcpkg install ${PKG}:${VCPKG_TARGET_TRIPLET} --overlay-triplets ${CMAKE_SOURCE_DIR}/vcpkg_triplets/windows
                    WORKING_DIRECTORY ${CMAKE_SOURCE_DIR}
                    RESULT_VARIABLE RESULT)
        elseif("${CMAKE_SYSTEM_NAME}" STREQUAL "Linux")
            execute_process(
                    COMMAND ${JCEF_VCPKG_DIRECTORY}/vcpkg install ${PKG}:${VCPKG_TARGET_TRIPLET}
                    WORKING_DIRECTORY ${CMAKE_SOURCE_DIR}
                    RESULT_VARIABLE RESULT)
        endif ()

        if (RESULT)
            message(FATAL_ERROR "Failed to install ${PKG}. Result: ${RESULT}")
        endif ()
    endforeach ()
endfunction()

function(vcpkg_bring_host_thrift)
    file(GLOB_RECURSE THRIFT_COMPILER_HOST
            ${CMAKE_SOURCE_DIR}/third_party/thrift/installed/*/tools/thrift/thrift
            ${CMAKE_SOURCE_DIR}/third_party/thrift/installed/*/tools/thrift/thrift.exe)
    if (THRIFT_COMPILER_HOST)
        LIST(LENGTH THRIFT_COMPILER_HOST N)
        if (${N} EQUAL 1)
            message("Found thrift compiler: ${THRIFT_COMPILER_HOST}")
            set(THRIFT_COMPILER_HOST ${THRIFT_COMPILER_HOST} PARENT_SCOPE)
            return()
        else ()
            message(FATAL_ERROR "Thrift compiler list is ambiguous '${THRIFT_COMPILER_HOST}'")
        endif ()
    endif ()

    message("Run: ${JCEF_VCPKG_DIRECTORY}/vcpkg install thrift")
    execute_process(
            COMMAND ${JCEF_VCPKG_DIRECTORY}/vcpkg install thrift
            WORKING_DIRECTORY ${CMAKE_SOURCE_DIR}
            RESULT_VARIABLE RESULT)
    if (RESULT)
        message(FATAL_ERROR "Failed to install thrift. Result: ${RESULT}")
    endif ()
    execute_process(
        COMMAND ${JCEF_VCPKG_DIRECTORY}/vcpkg export --raw --output=thrift_compiler --output-dir=${CMAKE_SOURCE_DIR}/third_party thrift
        WORKING_DIRECTORY ${CMAKE_SOURCE_DIR}
        RESULT_VARIABLE RESULT)

    file(GLOB_RECURSE THRIFT_COMPILER_HOST
            ${CMAKE_SOURCE_DIR}/third_party/thrift_compiler/installed/*/tools/thrift/thrift
            ${CMAKE_SOURCE_DIR}/third_party/thrift_compiler/installed/*/tools/thrift/thrift.exe
    )

    if (THRIFT_COMPILER_HOST)
        LIST(LENGTH THRIFT_COMPILER_HOST N)
        if (${N} EQUAL 1)
            message("Found thrift compiler: ${THRIFT_COMPILER_HOST}")
            set(THRIFT_COMPILER_HOST ${THRIFT_COMPILER_HOST} PARENT_SCOPE)
            return()
        else ()
            message(FATAL_ERROR "Thrift compiler list is ambiguous '${THRIFT_COMPILER_HOST}'")
        endif ()
    endif ()
endfunction()