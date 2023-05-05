#
# Generate client-server pairs
#
message(STATUS "Generate cpp thrift codegen...")
execute_process(
        COMMAND thrift --gen cpp -r ${CMAKE_CURRENT_SOURCE_DIR}/cef_server.thrift
        WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
)
execute_process(
        COMMAND thrift --gen cpp -r ${CMAKE_CURRENT_SOURCE_DIR}/cef_client.thrift
        WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
)
execute_process(
        COMMAND thrift --gen java -out ${CMAKE_SOURCE_DIR}/java/com/jetbrains/cef/remote ${CMAKE_CURRENT_SOURCE_DIR}/cef_server.thrift
        WORKING_DIRECTORY ${CMAKE_SOURCE_DIR}/java
)
execute_process(
        COMMAND thrift --gen java -out ${CMAKE_SOURCE_DIR}/java/com/jetbrains/cef/remote ${CMAKE_CURRENT_SOURCE_DIR}/cef_client.thrift
        WORKING_DIRECTORY ${CMAKE_SOURCE_DIR}/java
        RESULT_VARIABLE thrift_result)
message(STATUS "... result: ${thrift_result}")

set(CMAKE_CXX_STANDARD 14)
set(Boost_USE_STATIC_LIBS OFF)
set(Boost_USE_MULTITHREADED ON)
set(Boost_USE_STATIC_RUNTIME OFF)

find_package(Boost REQUIRED)

if(Boost_FOUND)
    include_directories(${Boost_INCLUDE_DIRS})
endif()

macro(REQUIRE_BOOST_HEADERS)
    find_package(Boost ${BOOST_MINREV} QUIET REQUIRED)
    if (NOT Boost_FOUND)
        message(FATAL_ERROR "Boost ${BOOST_MINREV} or later is required to build sources in ${CMAKE_CURRENT_SOURCE_DIR}")
    endif()
    if (DEFINED Boost_INCLUDE_DIRS)
        # pre-boost 1.70.0 aware cmake, otherwise it is using targets
        include_directories(SYSTEM "${Boost_INCLUDE_DIRS}")
    endif()
endmacro()
REQUIRE_BOOST_HEADERS()

find_package(Boost COMPONENTS filesystem REQUIRED)
find_package(log4cxx 1.0)

#
# Define helper libraries
#
set(thrift_codegen_SOURCES
    gen-cpp/Server.cpp
    gen-cpp/ClientHandlers.cpp
    gen-cpp/cef_client_types.cpp
)
add_library(thrift_codegen STATIC ${thrift_codegen_SOURCES})
link_directories(thrift_codegen /opt/homebrew/Cellar/thrift/0.16.0/lib)
target_include_directories(thrift_codegen PRIVATE /opt/homebrew/Cellar/thrift/0.16.0/include)
target_link_libraries(thrift_codegen thrift)

set(shared_mem_helper_SOURCES
    CefServerHelper.cpp
)
add_library(shared_mem_helper SHARED ${shared_mem_helper_SOURCES})
target_include_directories(shared_mem_helper PUBLIC ${JNI_INCLUDE_DIRS})

#
# Sources
#
set(CEFSIMPLE_HELPER_SRCS_MAC
    process_helper_mac.cc
)
APPEND_PLATFORM_SOURCES(CEFSIMPLE_HELPER_SRCS)

set(CEFSIMPLE_RESOURCES_MAC_SRCS_MAC
    mac/Info.plist
    mac/cefsimple.icns
)
APPEND_PLATFORM_SOURCES(CEFSIMPLE_RESOURCES_MAC_SRCS)

set(CEFSIMPLE_RESOURCES_MAC_ENGLISH_LPROJ_SRCS_MAC
    mac/English.lproj/InfoPlist.strings
    mac/English.lproj/MainMenu.xib
)
APPEND_PLATFORM_SOURCES(CEFSIMPLE_RESOURCES_MAC_ENGLISH_LPROJ_SRCS)

set(CEFSIMPLE_RESOURCES_SRCS
    ${CEFSIMPLE_RESOURCES_MAC_SRCS}
    ${CEFSIMPLE_RESOURCES_MAC_ENGLISH_LPROJ_SRCS}
)

#
# Set application
#
set(EXECUTABLE_NAME "CefServer")
set(PRODUCT_NAME "CefServer")
add_executable(${EXECUTABLE_NAME} MACOSX_BUNDLE ${CEFSIMPLE_RESOURCES_SRCS}
        CefBrowserAdapter.cpp
        CefBrowserAdapter.h
        CefUtils.cpp
        CefUtils.h
        CppServer.cpp
        KeyEventProcessing.cpp
        MouseEventProcessing.cpp
        ServerHandler.cpp
        Utils.cpp
        Utils.h
        log/Log.cpp
        log/Log.h
        handlers/RemoteClientHandler.cpp
        handlers/RemoteClientHandler.h
        handlers/RemoteRenderHandler.cpp
        handlers/RemoteRenderHandler.h
        handlers/RemoteLifespanHandler.cpp
        handlers/RemoteLifespanHandler.h
        handlers/RemoteLoadHandler.cpp
        handlers/RemoteLoadHandler.h
        handlers/RemoteAppHandler.cpp
        handlers/RemoteAppHandler.h
        handlers/RemoteBrowserProcessHandler.cpp
        handlers/RemoteBrowserProcessHandler.h)
add_dependencies(${EXECUTABLE_NAME} libcef_dll_wrapper)
target_include_directories(${EXECUTABLE_NAME} PRIVATE /opt/homebrew/Cellar/thrift/0.16.0/include)
target_include_directories(${EXECUTABLE_NAME} PRIVATE ${CEF_INCLUDE_PATH})
target_include_directories(${EXECUTABLE_NAME} PRIVATE $<TARGET_PROPERTY:log4cxx,INTERFACE_INCLUDE_DIRECTORIES>)
target_link_libraries(${EXECUTABLE_NAME} log4cxx)
target_link_libraries(${EXECUTABLE_NAME} libcef_dll_wrapper ${CEF_STANDARD_LIBS})
target_link_libraries(${EXECUTABLE_NAME} thrift_codegen)
target_link_libraries(${EXECUTABLE_NAME} thrift ${Boost_FILESYSTEM_LIBRARY})
if (ZLIB_FOUND)
    target_link_libraries(${EXECUTABLE_NAME} ${ZLIB_LIBRARIES})
endif ()
set_target_properties(${EXECUTABLE_NAME} PROPERTIES
    MACOSX_BUNDLE_INFO_PLIST ${CMAKE_CURRENT_SOURCE_DIR}/mac/Info.plist
)

set(APP_BUNDLE_PATH ${CMAKE_CURRENT_BINARY_DIR}/${EXECUTABLE_NAME}.app)
add_custom_command(TARGET ${EXECUTABLE_NAME}
    POST_BUILD
    COMMAND ${CMAKE_COMMAND} -E copy ${CMAKE_CURRENT_SOURCE_DIR}/log4cxx.logconfig ${APP_BUNDLE_PATH}/Contents/MacOS
)
add_custom_command(TARGET ${EXECUTABLE_NAME}
    POST_BUILD
    COMMAND ${CMAKE_COMMAND} -E copy_directory
    "${CEF_BINARY_DIR}/Chromium Embedded Framework.framework"
    "${APP_BUNDLE_PATH}/Contents/Frameworks/Chromium Embedded Framework.framework"
)

if(USE_SANDBOX)
    # Logical target used to link the cef_sandbox library.
    ADD_LOGICAL_TARGET("cef_sandbox_lib" "${CEF_SANDBOX_LIB_DEBUG}" "${CEF_SANDBOX_LIB_RELEASE}")
endif()

#
# Helper applications
#

set(CEF_HELPER_TARGET "${EXECUTABLE_NAME}_Helper")
set(CEF_HELPER_OUTPUT_NAME "${EXECUTABLE_NAME} Helper")

# Create the multiple Helper app bundle targets.
foreach(_suffix_list ${CEF_HELPER_APP_SUFFIXES})
    # Convert to a list and extract the suffix values.
    string(REPLACE ":" ";" _suffix_list ${_suffix_list})
    list(GET _suffix_list 0 _name_suffix)
    list(GET _suffix_list 1 _target_suffix)
    list(GET _suffix_list 2 _plist_suffix)

    # Define Helper target and output names.
    set(_helper_target "${CEF_HELPER_TARGET}${_target_suffix}")
    set(_helper_output_name "${CEF_HELPER_OUTPUT_NAME}${_name_suffix}")

    # Create Helper-specific variants of the helper-Info.plist file. Do this
    # manually because the configure_file command (which is executed as part of
    # MACOSX_BUNDLE_INFO_PLIST) uses global env variables and would insert the
    # wrong values with multiple targets.
    set(_helper_info_plist "${CMAKE_CURRENT_BINARY_DIR}/helper-Info${_target_suffix}.plist")
    file(READ "${CMAKE_CURRENT_SOURCE_DIR}/mac/helper-Info.plist" _plist_contents)
    string(REPLACE "\${EXECUTABLE_NAME}" "${_helper_output_name}" _plist_contents ${_plist_contents})
    string(REPLACE "\${PRODUCT_NAME}" "${_helper_output_name}" _plist_contents ${_plist_contents})
    string(REPLACE "\${BUNDLE_ID_SUFFIX}" "${_plist_suffix}" _plist_contents ${_plist_contents})
    file(WRITE ${_helper_info_plist} ${_plist_contents})

    # Create Helper executable target.
    add_executable(${_helper_target} MACOSX_BUNDLE ${CEFSIMPLE_HELPER_SRCS})
    SET_EXECUTABLE_TARGET_PROPERTIES(${_helper_target})
    add_dependencies(${_helper_target} libcef_dll_wrapper)
    target_link_libraries(${_helper_target} libcef_dll_wrapper ${CEF_STANDARD_LIBS})
    set_target_properties(${_helper_target} PROPERTIES
        MACOSX_BUNDLE_INFO_PLIST ${_helper_info_plist}
        OUTPUT_NAME ${_helper_output_name}
    )

    if(USE_SANDBOX)
        target_link_libraries(${_helper_target} cef_sandbox_lib)
    endif()

    # Add the Helper as a dependency of the main executable target.
    add_dependencies(${EXECUTABLE_NAME} "${_helper_target}")

    # Copy the Helper app bundle into the Frameworks directory.
    add_custom_command(
            TARGET ${EXECUTABLE_NAME}
            POST_BUILD
            COMMAND ${CMAKE_COMMAND} -E copy_directory
            "${CMAKE_CURRENT_BINARY_DIR}/${_helper_output_name}.app"
            "${APP_BUNDLE_PATH}/Contents/Frameworks/${_helper_output_name}.app"
            VERBATIM
    )
endforeach()
