#set(JCEF_VCPKG_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/third_party/vcpk)
#set(CMAKE_TOOLCHAIN_FILE "${CMAKE_CURRENT_SOURCE_DIR}/vcpkg/scripts/buildsystems/vcpkg.cmake"
#        CACHE STRING "Vcpkg toolchain file")

function(bring_vcpkg vcpkg_path)
    find_package(Git REQUIRED)
    execute_process(
            execute_process (COMMAND ${GIT_EXECUTABLE} submodule update --init --recursive
            WORKING_DIRECTORY ${PROJECT_SOURCE_DIR}
            COMMAND_ERROR_IS_FATAL ANY)
    )
endfunction()