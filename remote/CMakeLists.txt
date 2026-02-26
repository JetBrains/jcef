#
# Generate client-server pairs
#
set(CMAKE_VERBOSE_MAKEFILE ON)

set(Boost_NO_WARN_NEW_VERSIONS 1)
find_package(Boost REQUIRED COMPONENTS filesystem)
find_package(Thrift CONFIG REQUIRED)
vcpkg_bring_host_thrift()

SET_CEF_TARGET_OUT_DIR()

set(THRIFT_FILES
        cef_server.thrift
        cef_client.thrift
        shared.thrift)

foreach (FILE ${THRIFT_FILES})
    message(STATUS "Compile ${FILE} to C++...")
    execute_process(
            COMMAND ${THRIFT_COMPILER_HOST} --gen cpp -r ${CMAKE_CURRENT_SOURCE_DIR}/${FILE}
            WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
            RESULT_VARIABLE thrift_result
    )
    if (thrift_result AND NOT STATUS EQUAL 0)
        message(FATAL_ERROR "Failed to compile ${FILE}: ${thrift_result}")
    endif ()

    message(STATUS "Compile ${FILE} to Java...")
    execute_process(
            COMMAND ${THRIFT_COMPILER_HOST} --gen java:generated_annotations=suppress -out ${CMAKE_SOURCE_DIR}/java ${CMAKE_CURRENT_SOURCE_DIR}/${FILE}
            WORKING_DIRECTORY ${CMAKE_SOURCE_DIR}/java
            RESULT_VARIABLE thrift_result
    )
    if (thrift_result AND NOT STATUS EQUAL 0)
        message(FATAL_ERROR "Failed to compile ${FILE}: ${thrift_result}")
    endif ()
endforeach ()

message(STATUS "Patching java interfaces at ${CMAKE_SOURCE_DIR}/java/com/jetbrains/cef/remote/thrift_codegen...")
execute_process(
        COMMAND ${Python_EXECUTABLE} "${CMAKE_SOURCE_DIR}/jb/tools/common/patch_thrift_interfaces.py" "${CMAKE_SOURCE_DIR}/java/com/jetbrains/cef/remote/thrift_codegen/"
        RESULT_VARIABLE result
)

if (result AND NOT STATUS EQUAL 0)
    message(FATAL_ERROR "Failed to run the patch script: ${thrift_result}")
endif ()

set(CMAKE_CXX_STANDARD 17)
include_directories(${Boost_INCLUDE_DIRS})

if (OS_WINDOWS)
    add_compile_definitions(WIN32_LEAN_AND_MEAN)
endif ()

if (OS_MAC)
    set(Boost_USE_STATIC_LIBS OFF)
    set(Boost_USE_MULTITHREADED ON)
    set(Boost_USE_STATIC_RUNTIME OFF)
endif ()

if (OS_LINUX)
    add_link_options(-static-libstdc++)
endif ()

set(thrift_codegen_SOURCES
        gen-cpp/Server.cpp
        gen-cpp/ClientHandlers.cpp
        gen-cpp/cef_client_types.cpp
        gen-cpp/cef_server_types.cpp
        gen-cpp/shared_types.cpp
        )
add_library(thrift_codegen STATIC ${thrift_codegen_SOURCES})
if (OS_WINDOWS)
    set_library_target_properties(thrift_codegen)
endif ()
target_link_libraries(thrift_codegen thrift::thrift libcef_dll_wrapper ${CEF_STANDARD_LIBS})

if (OS_MAC)
    set(CEFSIMPLE_RESOURCES_MAC_SRCS_MAC
            mac/Info.plist
            )
    APPEND_PLATFORM_SOURCES(CEFSIMPLE_RESOURCES_MAC_SRCS)

    set(CEFSIMPLE_RESOURCES_MAC_ENGLISH_LPROJ_SRCS_MAC
            mac/English.lproj/InfoPlist.strings
            )
    APPEND_PLATFORM_SOURCES(CEFSIMPLE_RESOURCES_MAC_ENGLISH_LPROJ_SRCS)

    set(CEFSIMPLE_RESOURCES_SRCS
            ${CEFSIMPLE_RESOURCES_MAC_SRCS}
            ${CEFSIMPLE_RESOURCES_MAC_ENGLISH_LPROJ_SRCS})
endif ()

#
# Set application
#
set(EXECUTABLE_NAME "cef_server")
set(PRODUCT_NAME "cef_server")

set(SERVER_SOURCES
        main.cpp
        CefUtils.cpp
        CefUtils.h
        RemoteObjects.h
        RpcExecutor.cpp
        ServerApplication.cpp
        ServerHandler.cpp
        ServerHandlerContext.cpp
        ServerHandlerContext.cpp
        ServerHandlerContext.h
        Utils.cpp
        Utils.h
        browser/KeyEventProcessing.cpp
        browser/MouseEventProcessing.cpp
        browser/RemoteDevToolsMessageObserver.cpp
        browser/RemoteDevToolsMessageObserver.h
        browser/RemoteFrame.cpp
        browser/RemoteFrame.h
        callback/RemoteAuthCallback.h
        callback/RemoteCallback.h
        callback/RemoteCompletionCallback.cpp
        callback/RemoteCompletionCallback.h
        callback/RemoteIntCallback.cpp
        callback/RemoteIntCallback.h
        callback/RemoteRegistration.cpp
        callback/RemoteRegistration.h
        callback/RemoteSchemeHandlerFactory.cpp
        callback/RemoteStringVisitor.cpp
        callback/RemoteStringVisitor.h
        handlers/RemoteClientHandler.cpp
        handlers/RemoteClientHandler.h
        handlers/RemoteContextMenuHandler.cpp
        handlers/RemoteContextMenuHandler.h
        handlers/RemoteDisplayHandler.cpp
        handlers/RemoteDisplayHandler.h
        handlers/RemoteFocusHandler.cpp
        handlers/RemoteFocusHandler.h
        handlers/RemoteKeyboardHandler.cpp
        handlers/RemoteKeyboardHandler.h
        handlers/RemoteLifespanHandler.cpp
        handlers/RemoteLifespanHandler.h
        handlers/RemoteLoadHandler.cpp
        handlers/RemoteLoadHandler.h
        handlers/RemoteRenderHandler.cpp
        handlers/RemoteRenderHandler.h
        handlers/SharedBufferManager.cpp
        handlers/SharedBufferManager.h
        handlers/app/HelperApp.cpp
        handlers/app/HelperApp.h
        handlers/app/RemoteAppHandler.cpp
        handlers/app/RemoteAppHandler.h
        handlers/app/RemoteBrowserProcessHandler.cpp
        handlers/app/RemoteBrowserProcessHandler.h
        log/Log.cpp
        log/Log.h
        network/RemoteCookieAccessFilter.cpp
        network/RemoteCookieAccessFilter.h
        network/RemoteCookieManager.cpp
        network/RemoteCookieManager.h
        network/RemoteCookieVisitor.cpp
        network/RemoteCookieVisitor.h
        network/RemotePostData.cpp
        network/RemotePostData.h
        network/RemotePostDataElement.cpp
        network/RemotePostDataElement.h
        network/RemoteRequest.cpp
        network/RemoteRequest.h
        network/RemoteRequestContextHandler.cpp
        network/RemoteRequestContextHandler.h
        network/RemoteRequestHandler.cpp
        network/RemoteRequestHandler.h
        network/RemoteResourceHandler.cpp
        network/RemoteResourceHandler.h
        network/RemoteResourceRequestHandler.cpp
        network/RemoteResourceRequestHandler.h
        network/RemoteResponse.cpp
        network/RemoteResponse.h
        router/MessageRoutersManager.cpp
        router/MessageRoutersManager.h
        router/RemoteMessageRouter.cpp
        router/RemoteMessageRouter.h
        router/RemoteMessageRouterHandler.cpp
        router/RemoteMessageRouterHandler.h
        router/RemoteQueryCallback.h
        callback/RemoteCefRunContextMenuCallback.cpp
        callback/RemoteCefRunContextMenuCallback.h
        callback/RemoteCefRunContextMenuCallback.cpp
        callback/RemoteCefRunContextMenuCallback.h
        CefSettingsParser.cpp
        CefSettingsParser.h
        handlers/RemotePermissionHandler.cpp
        handlers/RemotePermissionHandler.h
        callback/RemoteMediaAccessCallback.h
        callback/RemoteRunFileDialogCallback.cpp
        callback/RemoteRunFileDialogCallback.h
        callback/RemotePdfPrintCallback.cpp
        callback/RemotePdfPrintCallback.h
        CrashHandler.cpp
        CrashHandler.h
        browser/RemoteBrowser.cpp
        browser/RemoteBrowser.h
        browser/RemoteClient.cpp
        browser/RemoteClient.h
)

if (OS_WINDOWS)
    list(APPEND SERVER_SOURCES
        windows/PipeTransport.cpp
        windows/PipeTransport.h
        windows/PipeTransportServer.cpp
        windows/PipeTransportServer.h
        ../native/critical_wait_win.cpp
        cef_server.rc
    )
    add_executable(${EXECUTABLE_NAME} ${SERVER_SOURCES})
    target_link_options(${EXECUTABLE_NAME} PRIVATE "/SUBSYSTEM:console")
endif ()

if (OS_MAC)
    list(APPEND SERVER_SOURCES mac/MacApplication.mm ../native/critical_wait_posix.cpp)
    add_executable(${EXECUTABLE_NAME} MACOSX_BUNDLE ${SERVER_SOURCES})
endif ()

if (OS_LINUX)
    list(APPEND SERVER_SOURCES ../native/critical_wait_posix.cpp)
    add_executable(${EXECUTABLE_NAME} ${SERVER_SOURCES})
endif ()

target_include_directories(${EXECUTABLE_NAME} PRIVATE ${CEF_INCLUDE_PATH})

if (OS_WINDOWS)
    add_logical_target("libcef_lib" "${CEF_LIB_DEBUG}" "${CEF_LIB_RELEASE}")
    target_link_libraries(${EXECUTABLE_NAME} libcef_lib "Dbghelp.dll")
endif ()

target_link_libraries(${EXECUTABLE_NAME} ${CEF_STANDARD_LIBS} thrift_codegen ${Boost_FILESYSTEM_LIBRARY})

if (OS_MAC)
    target_link_libraries(${EXECUTABLE_NAME} thrift::thrift)
endif ()

if (OS_WINDOWS)
    target_compile_options(${EXECUTABLE_NAME} PRIVATE /MP;/Gy;/GR;/W4;/WX;/wd4100;/wd4127;/wd4244;/wd4324;/wd4481;/wd4512;/wd4701;/wd4702;/wd4996;/wd4706;/wd4005;/wd4245;/Zi ${CEF_CXX_COMPILER_FLAGS})
    target_compile_options(${EXECUTABLE_NAME} PRIVATE $<$<CONFIG:Debug>:/MTd;/RTC1;/Od ${CEF_CXX_COMPILER_FLAGS_DEBUG}>)
    target_compile_options(${EXECUTABLE_NAME} PRIVATE $<$<CONFIG:Release>:/MT;/O2;/Ob2;/GF ${CEF_CXX_COMPILER_FLAGS_RELEASE}>)

    # Ensure PDB files are generated
    set(CMAKE_EXE_LINKER_FLAGS_DEBUG "${CMAKE_EXE_LINKER_FLAGS_DEBUG} /DEBUG")
    set(CMAKE_SHARED_LINKER_FLAGS_DEBUG "${CMAKE_SHARED_LINKER_FLAGS_DEBUG} /DEBUG")
    set(CMAKE_MODULE_LINKER_FLAGS_DEBUG "${CMAKE_MODULE_LINKER_FLAGS_DEBUG} /DEBUG")
    set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} /DEBUG")
    set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} /DEBUG")
    set(CMAKE_MODULE_LINKER_FLAGS "${CMAKE_MODULE_LINKER_FLAGS} /DEBUG")

    list(TRANSFORM CEF_COMPILER_DEFINES REPLACE "_HAS_EXCEPTIONS=0" "_HAS_EXCEPTIONS=1" OUTPUT_VARIABLE CEF_SERVER_COMPILER_DEFINES)
    message(STATUS "Added exception preprocessor definition. List of all defines: ${CEF_SERVER_COMPILER_DEFINES}")

    target_compile_definitions(${EXECUTABLE_NAME} PRIVATE ${CEF_SERVER_COMPILER_DEFINES})
    target_compile_definitions(${EXECUTABLE_NAME} PRIVATE $<$<CONFIG:Debug>:${CEF_COMPILER_DEFINES_DEBUG}>)
    target_compile_definitions(${EXECUTABLE_NAME} PRIVATE $<$<CONFIG:Release>:${CEF_COMPILER_DEFINES_RELEASE}>)

    target_include_directories(${EXECUTABLE_NAME} PRIVATE ${CEF_INCLUDE_PATH})

    if (CEF_LINKER_FLAGS)
        string(REPLACE ";" " " _flags_str "${CEF_LINKER_FLAGS}")
        set_property(TARGET ${EXECUTABLE_NAME} PROPERTY LINK_FLAGS ${_flags_str})
    endif ()
    if (CEF_LINKER_FLAGS_DEBUG)
        string(REPLACE ";" " " _flags_str "${CEF_LINKER_FLAGS_DEBUG}")
        set_property(TARGET ${EXECUTABLE_NAME} PROPERTY LINK_FLAGS_DEBUG ${_flags_str})
    endif ()
    if (CEF_LINKER_FLAGS_RELEASE)
        string(REPLACE ";" " " _flags_str "${CEF_LINKER_FLAGS_RELEASE}")
        set_property(TARGET ${EXECUTABLE_NAME} PROPERTY LINK_FLAGS_RELEASE ${_flags_str})
    endif ()

    set_target_properties(${EXECUTABLE_NAME}
            PROPERTIES
            RUNTIME_OUTPUT_DIRECTORY "${CEF_TARGET_OUT_DIR}/bin"
    )

    COPY_FILES("${EXECUTABLE_NAME}" "${CEF_BINARY_FILES}" "${CEF_BINARY_DIR}" "${CEF_TARGET_OUT_DIR}/bin")
    COPY_FILES("${EXECUTABLE_NAME}" "${CEF_RESOURCE_FILES}" "${CEF_RESOURCE_DIR}" "${CEF_TARGET_OUT_DIR}/lib")
    COPY_FILES("${EXECUTABLE_NAME}" "icudtl.dat" "${CEF_TARGET_OUT_DIR}/lib" "${CEF_TARGET_OUT_DIR}/bin")
endif ()

if (OS_LINUX)
    ADD_LOGICAL_TARGET("libcef_lib" "${CEF_LIB_DEBUG}" "${CEF_LIB_RELEASE}")
    COPY_FILES("${EXECUTABLE_NAME}" "${CEF_BINARY_FILES}" "${CEF_BINARY_DIR}" "${CEF_TARGET_OUT_DIR}")
    COPY_FILES(${EXECUTABLE_NAME} "${CEF_RESOURCE_FILES}" "${CEF_RESOURCE_DIR}" "${CEF_TARGET_OUT_DIR}")
    target_link_libraries(${EXECUTABLE_NAME} libcef_lib libcef_dll_wrapper rt ${CEF_STANDARD_LIBS})
    # Set rpath so that libraries can be placed next to the executable.
    set_target_properties(${EXECUTABLE_NAME} PROPERTIES INSTALL_RPATH "$ORIGIN")
    set_target_properties(${EXECUTABLE_NAME} PROPERTIES BUILD_WITH_INSTALL_RPATH TRUE)
endif ()

if (OS_MAC)
    set_target_properties(${EXECUTABLE_NAME} PROPERTIES
            MACOSX_BUNDLE_INFO_PLIST ${CMAKE_CURRENT_SOURCE_DIR}/mac/Info.plist)

    set(APP_BUNDLE_PATH ${CEF_TARGET_OUT_DIR}/${EXECUTABLE_NAME}.app)
    add_custom_command(TARGET ${EXECUTABLE_NAME}
            POST_BUILD
            COMMAND ${CMAKE_COMMAND} -E copy_directory
            "${CEF_BINARY_DIR}/Chromium Embedded Framework.framework"
            "${CEF_TARGET_OUT_DIR}/Chromium Embedded Framework.framework"
            )

    #
    # Helper applications
    #

    set(CEF_HELPER_TARGET "${EXECUTABLE_NAME}_Helper")
    set(CEF_HELPER_OUTPUT_NAME "${EXECUTABLE_NAME} Helper")

    set(CEFSIMPLE_HELPER_SRCS_MAC mac/process_helper_mac.cc handlers/app/HelperApp.cpp Utils.cpp)

    list(APPEND HELPER_EXTRA_COMPILER_FLAGS
            -fexceptions
            -frtti
            )

    # Create the multiple Helper app bundle targets.
    foreach (_suffix_list ${CEF_HELPER_APP_SUFFIXES})
        # Convert to a list and extract the suffix values.
        string(REPLACE ":" ";" _suffix_list ${_suffix_list})
        list(GET _suffix_list 0 _name_suffix)
        list(GET _suffix_list 1 _target_suffix)
        list(GET _suffix_list 2 _plist_suffix)

        if(_target_suffix MATCHES "alerts")
            message(STATUS "Skip building 'alerts' helper.")
            continue()
        endif()

        # Define Helper target and output names.
        set(_helper_target "${CEF_HELPER_TARGET}${_target_suffix}")
        set(_helper_output_name "${CEF_HELPER_OUTPUT_NAME}${_name_suffix}")

        # Create Helper-specific variants of the helper-Info.plist file. Do this
        # manually because the configure_file command (which is executed as part of
        # MACOSX_BUNDLE_INFO_PLIST) uses global env variables and would insert the
        # wrong values with multiple targets.
        set(_helper_info_plist "${CEF_TARGET_OUT_DIR}/helper-Info${_target_suffix}.plist")
        file(READ "${CMAKE_CURRENT_SOURCE_DIR}/mac/helper-Info.plist" _plist_contents)
        string(REPLACE "\${EXECUTABLE_NAME}" "${_helper_output_name}" _plist_contents ${_plist_contents})
        string(REPLACE "\${PRODUCT_NAME}" "${_helper_output_name}" _plist_contents ${_plist_contents})
        string(REPLACE "\${BUNDLE_ID_SUFFIX}" "${_plist_suffix}" _plist_contents ${_plist_contents})
        file(WRITE ${_helper_info_plist} ${_plist_contents})

        # Create Helper executable target.
        add_executable(${_helper_target} MACOSX_BUNDLE ${CEFSIMPLE_HELPER_SRCS_MAC})
        target_link_libraries(${_helper_target} thrift_codegen)
        target_link_libraries(${_helper_target} thrift::thrift ${Boost_FILESYSTEM_LIBRARY})
        SET_EXECUTABLE_TARGET_PROPERTIES(${_helper_target})
        target_compile_options(${_helper_target} PRIVATE ${HELPER_EXTRA_COMPILER_FLAGS})
        add_dependencies(${_helper_target} libcef_dll_wrapper)
        target_link_libraries(${_helper_target} libcef_dll_wrapper ${CEF_STANDARD_LIBS})
        set_target_properties(${_helper_target} PROPERTIES
                MACOSX_BUNDLE_INFO_PLIST ${_helper_info_plist}
                OUTPUT_NAME ${_helper_output_name}
                )

        # Add the Helper as a dependency of the main executable target.
        add_dependencies(${EXECUTABLE_NAME} "${_helper_target}")

        # Copy the Helper app bundle into the Frameworks directory.
        add_custom_command(
                TARGET ${EXECUTABLE_NAME}
                POST_BUILD
                COMMAND ${CMAKE_COMMAND} -E copy_directory
                "${CEF_TARGET_OUT_DIR}/${_helper_output_name}.app"
                "${APP_BUNDLE_PATH}/Contents/Frameworks/${_helper_output_name}.app"
                VERBATIM
        )
    endforeach ()
endif ()

set(shared_mem_helper_SOURCES
        shared_mem_helper.cpp
        ../native/keyboard_utils.cpp
        ../native/keyboard_utils.h
        PlatformUtils.cpp
)

set(shared_mem_helper_LIBS)

if (OS_WINDOWS)
    list(APPEND shared_mem_helper_SOURCES
            windows/WindowsPipe.cpp
            PlatformUtilsWin.cpp
    )
elseif (OS_LINUX)
    list(APPEND shared_mem_helper_SOURCES
            ../native/JavaKeyCode2X11.cpp
            PlatformUtilsLinux.cpp
    )
elseif (OS_MAC)
    list(APPEND shared_mem_helper_SOURCES
            PlatformUtilsMac.mm
    )

    find_library(COREFOUNDATION_FRAMEWORK CoreFoundation)
    find_library(FOUNDATION_FRAMEWORK Foundation)
    find_library(IOSURFACE_FRAMEWORK IOSurface)
    find_library(METAL_FRAMEWORK Metal)
    list(APPEND shared_mem_helper_LIBS
            "${COREFOUNDATION_FRAMEWORK}"
            "${FOUNDATION_FRAMEWORK}"
            "${IOSURFACE_FRAMEWORK}"
            "${METAL_FRAMEWORK}"
    )
endif ()

add_library(shared_mem_helper SHARED ${shared_mem_helper_SOURCES})
target_include_directories(shared_mem_helper PUBLIC ${JNI_INCLUDE_DIRS})
target_link_libraries(shared_mem_helper ${shared_mem_helper_LIBS})

if (OS_LINUX)
    target_link_libraries(shared_mem_helper rt)

    # Export only JNI symbols (especially important when statically linking other libraries, e.g., libstdc++)
    # Test symbol export with: nm -D --defined-only libshared_mem_helper.so
    set(VERSION_SCRIPT ${CMAKE_CURRENT_SOURCE_DIR}/shared_mem_helper_exports.map)
    target_link_options(shared_mem_helper PRIVATE
        -Wl,--version-script,${VERSION_SCRIPT}
        -Wl,--no-undefined-version
        )
    set_target_properties(shared_mem_helper PROPERTIES LINK_DEPENDS ${VERSION_SCRIPT})

endif ()
