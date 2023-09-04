#set(CMAKE_TOOLCHAIN_FILE "${CMAKE_CURRENT_SOURCE_DIR}/vcpkg/scripts/buildsystems/vcpkg.cmake"
#        CACHE STRING "Vcpkg toolchain file")

function(bring_vcpkg)
    set(JCEF_VCPKG_DIRECTORY ${CMAKE_SOURCE_DIR}/third_party/vcpkg)

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
    elseif ("${CMAKE_HOST_SYSTEM_NAME}" STREQUAL "Darwin")
        set(VCPKG_TARGET_TRIPLET "${VCPKG_ARCH}-osx-jcef")
    else ()
        message(FATAL_ERROR "Unknown OS: ${CMAKE_HOST_SYSTEM_NAME}")
    endif ()

    set(JCEF_VCPKG_DIRECTORY ${JCEF_VCPKG_DIRECTORY} PARENT_SCOPE)
    set(CMAKE_TOOLCHAIN_FILE "${JCEF_VCPKG_DIRECTORY}/scripts/buildsystems/vcpkg.cmake" PARENT_SCOPE)
    set(VCPKG_TARGET_TRIPLET ${VCPKG_TARGET_TRIPLET} PARENT_SCOPE)
endfunction()

function(vcpkg_install_package)
    foreach (PKG IN LISTS ARGN)
        message("Run: ${JCEF_VCPKG_DIRECTORY}/vcpkg install ${PKG}:${VCPKG_TARGET_TRIPLET}")
        execute_process(
                COMMAND ${JCEF_VCPKG_DIRECTORY}/vcpkg install ${PKG}:${VCPKG_TARGET_TRIPLET} --overlay-triplets ${CMAKE_SOURCE_DIR}/vcpkg_triplets/mac
                WORKING_DIRECTORY ${CMAKE_SOURCE_DIR}
                RESULT_VARIABLE RESULT)
        if (RESULT)
            message(FATAL_ERROR "Failed to install ${PKG}. Result: ${RESULT}")
        endif ()
    endforeach ()
endfunction()