#set(CMAKE_TOOLCHAIN_FILE "${CMAKE_CURRENT_SOURCE_DIR}/vcpkg/scripts/buildsystems/vcpkg.cmake"
#        CACHE STRING "Vcpkg toolchain file")

set(JCEF_VCPKG_DIRECTORY ${CMAKE_SOURCE_DIR}/third_party/vcpkg)
function(bring_vcpkg)
    find_package(Git REQUIRED)
    execute_process(
            COMMAND ${GIT_EXECUTABLE} submodule update --init --recursive
            WORKING_DIRECTORY ${CMAKE_SOURCE_DIR}
            COMMAND_error_is_fatal ANY)
    if(OS_WINDOWS)
        execute_process(
                COMMAND cmd /C ${JCEF_VCPKG_DIRECTORY}/bootstrap-vcpkg.bat -disableMetrics
                WORKING_DIRECTORY ${JCEF_VCPKG_DIRECTORY}
                COMMAND_ERROR_IS_FATAL ANY)
    else ()
        execute_process(
                COMMAND bash ${JCEF_VCPKG_DIRECTORY}/bootstrap-vcpkg.sh -disableMetrics
                WORKING_DIRECTORY ${JCEF_VCPKG_DIRECTORY}
                COMMAND_ERROR_IS_FATAL ANY)
    endif()
endfunction()